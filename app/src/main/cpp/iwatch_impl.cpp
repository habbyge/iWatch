//
// Created by 葛祥林 on 1/8/21.
//

#include "iwatch_impl.h"
#include "ArtRestore.h"

//#include <art/runtime/jni/jni_internal.h>
//#include <exception> C/C++ 的 Exception

// -----------------------------------------------------------------------------------------------
// 查看 art 虚拟机代码技巧：
// 1. Java中的class全名(例如：java/lang/reflect/Method)，对应 Art 中的源文件是：
// art/runtime/native/java_lang_reflect_Method.h
// art/runtime/native/java_lang_reflect_Method.cc
// 即标准的 JNI 接口协议.
// -----------------------------------------------------------------------------------------------

// 方案限制：inline 函数 fix 会失败.
// 这里需要考虑，对 inline 函数的影响，内联函数是否有 Method 对象，是否能够被成功替换 ？
// 需要研究：直接 ArtMethod 整体替换的方案，是否能够解决 inline 问题 ？
// 这篇文章对 inline 的影响有详尽研究(同时也收录到了我的有道云笔记中了)：
// https://cloud.tencent.com/developer/article/1005604x
// 内联的源代码位置: art/compiler/optimizing/inliner.cc 中的 HInliner::Run() 函数中.
// 实际上，针对 "数据统计补丁" 来说，一般很少有机会在 inline 方法中插桩，即使需要，也可以通过其他手段突破.
// 因此，作为 局限性之一的 inline fix 失败，实际上对 数据统计补丁来说，影响并不大，凡是并不仅能尽善尽美，接收缺陷.
// 我研究了一阵子，就 replace inline 函数来说，从 native 层来说，几乎无解。。。
// 如果在 Java 层给每一个函数插桩一个 try-catch，来阻止编译期的 inline，实际上是非常糟糕的做法。。不能被接受
// 实际上所有的方案，都不能尽善尽美，做不到 inline 的话，统计上报需求来了后，我们自己评估下，如果需要在可能被
// inline 的方法中插桩的话，我们就通过发版本方式来统计。。其实也还好.
// 我 review 了下，我之前的那么多统计上报代码，在可能被 inline 的地方添加上报的可能性几乎么有。

// Android-11 适配问题 ------> fix

// jni.h 中的实现，在 Art 虚拟机中的目录是 art/runtime/jni/ 目录下，例如：art/runtime/jni/jni_internal.cc

// TODO: dex diff 问题

/**
 * 这里仅仅只有一个目的，就是为了计算出不同平台下，每个 art::mirror::ArtMethod 大小，
 * 这里 jmethodID 就是 ArtMethod.
 * 比起 ArtFix，iWatch 方案屏蔽细节、尽量通用，没有适配性。
 */
static size_t artMethodSizeV1 = -1;
static size_t artMethodSizeV2 = -1;

static int sdkVersion = 0;
static void* cur_thread = nullptr;
static JavaVM* vm;

//using addWeakGlobalRef_t = jweak (*) (JavaVM*, void*, art::ObjPtr<art::mirror::Object>);
//addWeakGlobalRef_t addWeakGlobalRef;

// Android-11：
// art/runtime/jni/check_jni.cc
// ArtMethod* CheckMethodID(jmethodID mid)
//using CheckMethodID_t = art::mirror::ArtMethod_11* (*)(jmethodID);
//CheckMethodID_t CheckMethodID;
// 本来这个符号挺好的，但是 nm 一下发现是 t(小t) 类型的，这样的话，是没有导出的，不能使用，命令是：
// nm -extern-only libart.so | grep CheckMethodID 或 nm -g libart.so | grep CheckMethodID
// 一般情况下，写在 .h 文件中的 或 extern 声明的 函数才被导出.
//static const char* CheckMethodID_Sym = "_ZN3art12_GLOBAL__N_111ScopedCheck13CheckMethodIDEP10_jmethodID";

// 类似的还有 art/runtime/jni/jni_internal.h 中的 DecodeArtMethod，这个不适合的原因是：
// ALWAYS_INLINE
// static inline jmethodID EncodeArtMethod(ReflectiveHandle<ArtMethod> art_method)
// 被声明为 inline，所以不会被导出到外部符号(虽然也在.h文件中).
// 所以，查阅art源码时，找适合的函数符号必须排除：inline函数、没有使用extern或在.h中声明的函数，实际上使用:
// nm -g libart.so | grep '符号名' 查看一下类型更靠谱.

// ==
//static const char* DecodeMethodId_sym = "_ZN3art3jni12JniIdManager14DecodeMethodIdEP10_jmethodID";
//using DecodeMethodId_t = void* (*)(jmethodID);
//DecodeMethodId_t DecodeMethodId;
//static const char* GetGenericMap_Sym =
//    "_ZN3art3jni12JniIdManager13GetGenericMapINS_9ArtMethodEEERNSt3__16vectorIPT_NS4_9allocatorIS7_EEEEv";

// 方案1:
// art/runtime/art_method.h
// static ArtMethod* FromReflectedMethod(const ScopedObjectAccessAlreadyRunnable& soa, jobject jlr_method);
// 好处是：static 函数
FromReflectedMethod_t FromReflectedMethod;

// 方案2:
// art/runtime/jni/jni_internal.h
// ArtMethod* FindMethodJNI(const ScopedObjectAccess& soa,
//                         jclass java_class,
//                         const char* name,
//                         const char* sig,
//                         bool is_static);
// 好处是：不是class文件，是全局函数的非类文件
FindMethodJNI_t FindMethodJNI;
// 同时使用 方案1 和 方案2，哪个生效用哪里，双保险!!!!!!

ArtRestore* artRestore;

/**
 * 方案1
 */
static void* getArtMethod(JNIEnv* env, jobject method) {
  try {
    const art::ScopedFastNativeObjectAccess soa(env);
    return FromReflectedMethod(soa, method);
  } catch (std::exception& e) {
    loge("getArtMethod1, eception: %s", e.what());
    clear_exception(env);
    return nullptr;
  }
}

/**
 * 方案2
 */
static void* getArtMethod(JNIEnv* env, jclass java_class, const char* name, const char* sig, bool is_static) {
  try {
    art::ScopedObjectAccess soa(env);
    return FindMethodJNI(soa, java_class, name, sig, is_static);
  } catch (std::exception& e) {
    loge("getArtMethod2, eception: %s", e.what());
    clear_exception(env);
    return nullptr;
  }
}

static void initArtMethod1(JNIEnv* env, void* context, jobject m1, jobject m2) {
  try {
    FromReflectedMethod = reinterpret_cast<FromReflectedMethod_t>(dlsym_elf(context, FromReflectedMethod_Sym));
    logi("initArtMethod1, FromReflectedMethod=%p", FromReflectedMethod);
    void* artMethod1 = getArtMethod(env, m1);
    logi("initArtMethod1, method1=%p, %p", artMethod1, m1);
    void* artMethod2 = getArtMethod(env, m2);
    // jmethodID, jmethodID, ArtMethod*, ArtMethod*
    logi("initArtMethod1, method2=%p, %p", artMethod2, m2);

    // 这里动态获取 ArtMethod 大小的原理是：在 Art 虚拟机中的 Class 类中的 methods_ 字段决定的：
    // ArtMethod按照类中方法声明顺序依次紧密的排列在 methods_ 字段表示的内存中.
    artMethodSizeV1 = reinterpret_cast<size_t>(artMethod2) - reinterpret_cast<size_t>(artMethod1);
    logi("initArtMethod1, artMethodSizeV1 = %zu", artMethodSizeV1);
  } catch (std::exception& e) {
    loge("initArtMethod1, eception: %s", e.what());
    clear_exception(env);
    artMethodSizeV1 = -1;
  }
}

static void initArtMethod2(JNIEnv* env, void* context) {
  try {
    jclass ArtMethodSizeClass = env->FindClass(computeArtMethodSize_ClassName);
    FindMethodJNI = reinterpret_cast<FindMethodJNI_t>(dlsym_elf(context, FindMethodJNI_Sym));
    logi("initArtMethod2, FindMethodJNI=%p", FindMethodJNI);
    void* artMethod11 = getArtMethod(env, ArtMethodSizeClass, "func1", "()V", true);
    logi("initArtMethod2, artMethod11=%p", artMethod11);
    void* artMethod12 = getArtMethod(env, ArtMethodSizeClass, "func2", "()V", true);
    // jmethodID, jmethodID, ArtMethod*, ArtMethod*
    logi("initArtMethod2, artMethod22=%p", artMethod12);

    // 这里动态获取 ArtMethod 大小的原理是：在 Art 虚拟机中的 Class 类中的 methods_ 字段决定的
    artMethodSizeV2 = reinterpret_cast<size_t>(artMethod12) - reinterpret_cast<size_t>(artMethod11);
    logi("initArtMethod2, artMethodSizeV2 = %zu", artMethodSizeV2);
  } catch (std::exception& e) {
    loge("initArtMethod2, eception: %s", e.what());
    clear_exception(env);
    artMethodSizeV2 = -1;
  }
}

void init_impl(JNIEnv* env, int sdkVersionCode, jobject m1, jobject m2) {
  sdkVersion = sdkVersionCode;

  env->GetJavaVM(&vm);

  artRestore = new ArtRestore();

  // art::mirror::ArtMethod
//  auto artMethod11 = env->FromReflectedMethod(m1);
//  auto artMethod22 = env->FromReflectedMethod(m2);
//  artMethodSize = (size_t) artMethod22 - (size_t) artMethod11;
//  logd("iwatch init artMethodSize, success=%zu, %zu, %zu", artMethodSize,
//       (size_t) artMethod22,
//       (size_t) artMethod11);

  if (sdkVersionCode <= 29) { // <= Android-10(api-29)
    jclass ArtMethodSizeClass = env->FindClass(computeArtMethodSize_ClassName);
    auto artMethod1 = env->GetStaticMethodID(ArtMethodSizeClass, "func1", "()V");
    auto artMethod2 = env->GetStaticMethodID(ArtMethodSizeClass, "func2", "()V");
    artMethodSizeV1 = reinterpret_cast<size_t>(artMethod2) - reinterpret_cast<size_t>(artMethod1);

    // artMethodSize = sizeof(ArtMethod);
    logi("artMethodSize-1 = %d, %zu, %zu, %zu", sdkVersionCode, artMethodSizeV1,
                                                reinterpret_cast<size_t>(artMethod2),
                                                reinterpret_cast<size_t>(artMethod1));
  } else { // >= Android-11(api-30)
    loge("iwatch init, sdk >= API-30(Android-11): %d", sdkVersionCode);

    // 在 <= Android-10之前的版本，jni id 都是kPointer类型的，在 Andorid-11 之后都是 kIndices 类型了，
    // 所以，>= Android-11 的版本中，jmethodID != ArtMethod*了，art源码中是：art/runtime/jni_id_type.h
    // enum class JniIdType {
    //  // All Jni method/field IDs are pointers to the corresponding Art{Field,Method} type
    //  kPointer,
    //  // All Jni method/field IDs are indices into a table.
    //  kIndices, // here !!!!!!
    //  // All Jni method/field IDs are pointers to the corresponding Art{Field,Method} type but we
    //  // keep around extra information support changing modes to either kPointer or kIndices later.
    //  kSwapablePointer,
    //  kDefault = kPointer,
    //};

    // [技术方案原理]:
    // review 代码发现这里与之前不同：
    // 代码路径：art/runtime/jni/jni_internal.h
    // - Android-11
    // template <bool kEnableIndexIds = true>
    // ALWAYS_INLINE
    // static inline ArtMethod* DecodeArtMethod(jmethodID method_id) {
    //   if (IsIndexId<kEnableIndexIds>(method_id)) {
    //     return Runtime::Current()->GetJniIdManager()->DecodeMethodId(method_id);
    //   } else {
    //     return reinterpret_cast<ArtMethod*>(method_id);
    //   }
    // }
    // - Android-10
    // ALWAYS_INLINE
    // static inline ArtMethod* DecodeArtMethod(jmethodID method_id) {
    //   return reinterpret_cast<ArtMethod*>(method_id);
    // }
    // 很明显可以看到，Android-10 中的 jmethodID 与 ArtMethod* 相等；Android-11 中的就不一定了

    // art/runtime/jni/jni_id_manager.h/cc 中:
    // ArtMethod* DecodeMethodId(jmethodID method) REQUIRES(!Locks::jni_id_lock_);
    //
    // 方案1:
    jclass ArtMethodSizeClass = env->FindClass(computeArtMethodSize_ClassName);
    auto jmethodID1 = env->GetStaticMethodID(ArtMethodSizeClass, "func1", "()V");
    auto jmethodID2 = env->GetStaticMethodID(ArtMethodSizeClass, "func2", "()V");

//    auto IsIndexId1 = (reinterpret_cast<uintptr_t>(methodid1) % 2) != 0;
//    auto IsIndexId2 = (reinterpret_cast<uintptr_t>(methodid2) % 2) != 0;
//    logi("artMethodSize-2 = %zu, %zu, %zu, %zu, %d, %d", artMethodSize,
//                                                  reinterpret_cast<uintptr_t>(ArtMethodSizeClass),
//                                                  reinterpret_cast<uintptr_t>(methodid1),
//                                                  reinterpret_cast<uintptr_t>(methodid2),
//                                                  IsIndexId1, IsIndexId2);

    void* context = dlopen_elf("libart.so", RTLD_NOW);
    logi("dlopen_elf: %p", context);
    if (context == nullptr) {
      return;
    }

    // 注意 libart.so 中的符号都是加过密的
//    const char* addWeakGloablRef_Sym =
//        sdkVersionCode <= 25 ? "_ZN3art9JavaVMExt16AddWeakGlobalRefEPNS_6ThreadEPNS_6mirror6ObjectE"
//          : "_ZN3art9JavaVMExt16AddWeakGlobalRefEPNS_6ThreadENS_6ObjPtrINS_6mirror6ObjectEEE";
//      //  这个也符合Android-11
//    addWeakGlobalRef = reinterpret_cast<addWeakGlobalRef_t>(dlsym_elf(context, addWeakGloablRef_Sym));
//    logi("init, addWeakGlobalRef=%p", addWeakGlobalRef);
//
//    jweak weakRef1 = addWeakGlobalRef(vm, (void*) cur_thread, art::ObjPtr<art::mirror::Object>(m1));
//    jweak weakRef2 = addWeakGlobalRef(vm, (void*) cur_thread, art::ObjPtr<art::mirror::Object>(m2));
//    logi("init, weakRef=%p, weakRef2=%p", weakRef1, weakRef2);

//    CheckMethodID = reinterpret_cast<CheckMethodID_t>(dlsym_elf(context, CheckMethodID_Sym));
//    logi("init, CheckMethodID=%p", CheckMethodID);
//    void* artMethod1 = CheckMethodID(jmethodID1);
//    void* artMethod2 = CheckMethodID(jmethodID2);
//    logi("init, artMethod1=%p, artMethod2=%p", artMethod1, artMethod2);

//    DecodeMethodId = reinterpret_cast<DecodeMethodId_t>(dlsym_elf(context, DecodeMethodId_sym));
//    logi("init, DecodeMethodId=%p", DecodeMethodId);
//    void* artMethod1 = DecodeMethodId(jmethodID1);
//    void* artMethod2 = DecodeMethodId(jmethodID2);
//    // jmethodID, jmethodID, ArtMethod*, ArtMethod*
//    logi("init, method1=%p, %p, method2=%p, %p", jmethodID1, jmethodID2, artMethod1, artMethod2);

    // 实际上该函数(方法)符号，本质上在art虚拟机的二进制文件(libart.so)中的一个地址，更进一步说，我们传入该函数
    // 符号地址对应的该类型的参数即可，这里的参数类型在libart.so中是二进制形式存在，即不关心这个参数类型是art虚拟
    // 机目录中的，还是我们在外部工程中自己定义的，只要二进制表现形式一致，且参数值正确即可，所以这里的技术点之一即
    // 是：自己以 "从简的方式" 定义其对应的参数类型在art目录中的形式即可，再直白点就是：查看art虚拟机源代码，根据
    // 需要抄过来其实现即可。
    // 方案1：
    initArtMethod1(env, context, m1, m2);

    // 方案2
    initArtMethod2(env, context);

    dlclose_elf(context); // 释放

    /*artMethodSize = reinterpret_cast<size_t>(artMethod2) - reinterpret_cast<size_t>(artMethod1);*/
//    artMethodSize = sizeof(art::mirror::ArtMethod_11); // 40-bytes

//    void* artMethod1 = art::jni::DecodeArtMethod(methodid1);
//    void* artMethod2 = art::jni::DecodeArtMethod(methodid2);
//    logi("artMethodSize-2 = %zu, %zu, %zu", artMethodSize,
//                                            reinterpret_cast<size_t>(artMethod1),
//                                            reinterpret_cast<size_t>(artMethod2));

    // 方案2:
//    artMethodSize = sizeof(art::mirror::art_method_11);
//    logi("artMethodSize = %zu", artMethodSize);
  }

  clear_exception(env);
}

/**
 * 采用整体替换方法结构(art::mirror::ArtMethod)，忽略底层实现，从而解决兼容稳定性问题，
 * 比AndFix稳定可靠.
 * 旧的方案ArtMethod中的
 *
 * sizeof(ArtMethod) 的原理:
 * https://developer.aliyun.com/article/74598
 * https://cloud.tencent.com/developer/article/1329595
 * 即：art/runtime/class_linker.cc 中的: ClassLinker::AllocArtMethodArray中按线性分配ArtMethod大小
 * 逻辑在 ClassLinker::LoadClass 中.
 */
long method_hook_impl(JNIEnv* env, jstring srcClass, jstring srcName, jstring srcSig,
                      jobject srcMethod, jobject dstMethod) {

  void* srcArtMethod = nullptr;
  void* dstArtMethod = nullptr;

  if (sdkVersion <= 29) { // <= Android-10
    // art::mirror::ArtMethod
    srcArtMethod = reinterpret_cast<void*>(env->FromReflectedMethod(srcMethod));
    dstArtMethod = reinterpret_cast<void*>(env->FromReflectedMethod(dstMethod));
    logd("method_hook(api<=29), srcArtMethod=%p, dstArtMethod=%p", srcArtMethod, dstArtMethod);
    if (srcArtMethod == nullptr || dstArtMethod == nullptr) {
      loge("method_hook(api<=29), srcArtMethod/dstArtMethod is nullptr !");
      clear_exception(env);
      return -1L;
    }
    // 这里有坑，大小不正确...... 在 Android-11 系统中这里的大小获取失败，错误！！！！！！
    // 经过研究 android-11.0.0_r17(http://aosp.opersys.com/) 源代码，class类中的ArtMethod
    // 排布依旧是线性分配的，这里没问题，问题是：从 FromReflectedMethod 这里获取地址是错误的(是个52/53很小的数值，
    // 一看就不是地址).
  } else {
    srcArtMethod = getArtMethod(env, srcMethod);
    dstArtMethod = getArtMethod(env, dstMethod);
    logd("method_hook(api>=30), srcArtMethod=%p, dstArtMethod=%p", srcArtMethod, dstArtMethod);
    if (srcArtMethod == nullptr || dstArtMethod == nullptr) {
      loge("method_hook(api>=30), srcArtMethod/dstArtMethod is nullptr !");
      clear_exception(env);
      return -1L;
    }
  }

  int8_t* backupArtMethod = nullptr;
  try {
    backupArtMethod = new int8_t[artMethodSizeV1];
    // 备份原方法
    memcpy(backupArtMethod, srcArtMethod, artMethodSizeV1);
    // 替换成新方法
    memcpy(srcArtMethod, dstArtMethod, artMethodSizeV1);
    logd("method_hook, coppy: artMethodSizeV1=%zu", artMethodSizeV1);
  } catch (std::exception& e) {
    loge("method_hook copy: eception: %s", e.what());
    clear_exception(env);
    return -1L;
  }

  jboolean isCopy;
  auto srcClassChars = env->GetStringUTFChars(srcClass, &isCopy);
  std::string srcClassStr(srcClassChars);
  env->ReleaseStringUTFChars(srcClass, srcClassChars);
  auto srcFuncChars = env->GetStringUTFChars(srcName, &isCopy);
  std::string srcFuncStr(srcFuncChars);
  env->ReleaseStringUTFChars(srcName, srcFuncChars);
  auto srcDescChars = env->GetStringUTFChars(srcSig, &isCopy);
  std::string srcDescStr(srcDescChars);
  env->ReleaseStringUTFChars(srcSig, srcDescChars);
  auto backupArtMethodAddr = reinterpret_cast<long>(backupArtMethod);
  auto srcArtMethodAddr = reinterpret_cast<long>(srcArtMethod);
  artRestore->save(srcClassStr, srcFuncStr, srcDescStr, backupArtMethodAddr, srcArtMethodAddr);

  logi("methodHook: method_hook Success !");
  clear_exception(env);

  // 返回需要备份的原方法数据内容的地址，注意：这里 new 的对象在 restore 逻辑中才需要回收(delete[])
  return backupArtMethodAddr;
}

long method_hookv2_impl(JNIEnv* env,
                        jstring java_class1, jstring name1, jstring sig1, jboolean is_static1,
                        jstring java_class2, jstring name2, jstring sig2, jboolean is_static2) {

  if (sdkVersion <= 29) { // <= Android-10
    loge("method_hookv2 sdkVersion NOT >= 30: %d", sdkVersion);
    clear_exception(env);
    return -1L;
  }

  if (java_class1 == nullptr || java_class2 == nullptr
      || name1 == nullptr || name2 == nullptr
      || sig1 == nullptr || sig2 == nullptr) {

    loge("method_hookv2 input params are nullptr !");
    clear_exception(env);
    return -1L;
  }

  jboolean isCopy;
  const char* class1 = env->GetStringUTFChars(java_class1, &isCopy);
  jclass jclass1;
  std::string classStr1(class1);
  std::replace(classStr1.begin(), classStr1.end(), '.', '/');
  try {
    jclass1 = env->FindClass(classStr1.c_str());
  } catch (std::exception& e) {
    loge("method_hookv2 FindClass-1: %s", e.what());
    clear_exception(env);
    return -1L;
  }

  const char* name_str1 = env->GetStringUTFChars(name1, &isCopy);
  const char* sig_str1 = env->GetStringUTFChars(sig1, &isCopy);
  auto artMethod1 = getArtMethod(env, jclass1, name_str1, sig_str1, is_static1);
  env->ReleaseStringUTFChars(java_class1, class1);
  env->ReleaseStringUTFChars(name1, name_str1);
  env->ReleaseStringUTFChars(sig1, sig_str1);
  logi("method_hookv2, artMethod1=%p", artMethod1);
  if (artMethod1 == nullptr) {
    clear_exception(env);
    return -1L;
  }

  const char* class2 = env->GetStringUTFChars(java_class2, &isCopy);
  jclass jclass2;
  std::string classStr2(class2);
  std::replace(classStr2.begin(), classStr2.end(), '.', '/');
  try {
    jclass2 = env->FindClass(classStr2.c_str());
  } catch (std::exception& e) {
    loge("method_hookv2 FindClass-2: %s", e.what());
    clear_exception(env);
    return -1L;
  }

  const char* name_str2 = env->GetStringUTFChars(name2, &isCopy);
  const char* sig_str2 = env->GetStringUTFChars(sig2, &isCopy);
  auto artMethod2 = getArtMethod(env, jclass2, name_str2, sig_str2, is_static2);
  env->ReleaseStringUTFChars(java_class2, class2);
  env->ReleaseStringUTFChars(name2, name_str2);
  env->ReleaseStringUTFChars(sig2, sig_str2);
  logi("method_hookv2, artMethod2=%p", artMethod2);
  if (artMethod2 == nullptr) {
    clear_exception(env);
    return -1L;
  }

  int8_t* backupArtMethod = nullptr;
  try {
    backupArtMethod = new int8_t[artMethodSizeV2];
    memcpy(backupArtMethod, artMethod1, artMethodSizeV2);
    memcpy(artMethod1, artMethod2, artMethodSizeV2);
  } catch (std::exception& e) {
    loge("method_hookv2 copy: eception: %s", e.what());
    clear_exception(env);
    return -1L;
  }

  auto srcFunc = env->GetStringUTFChars(name1, &isCopy);
  std::string srcFuncStr(srcFunc);
  env->ReleaseStringUTFChars(name1, srcFunc);
  auto srcDesc = env->GetStringUTFChars(sig1, &isCopy);
  std::string srcDescStr(srcDesc);
  env->ReleaseStringUTFChars(sig1, srcDesc);
  auto backupArtMethodAddr = reinterpret_cast<long>(backupArtMethod);
  auto srcArtMethodAddr = reinterpret_cast<long>(artMethod1);
  artRestore->save(classStr1, srcFuncStr, srcDescStr, backupArtMethodAddr, srcArtMethodAddr);

  logi("method_hookv2: method_hook Success !");
  clear_exception(env);

  // 返回需要备份的原方法数据内容的地址，注意：这里 new 的对象在 restore 逻辑中才需要回收(delete[])
  return backupArtMethodAddr;
}

long restore_method_impl(JNIEnv* env, jstring className, jstring name, jstring sig,
                         long srcArtMethodAddr, long backupArtMethodData) {

  void* srcArtMethodPtr = reinterpret_cast<void*>(srcArtMethodAddr);
  void* backupArtMethod = reinterpret_cast<void*>(backupArtMethodData);
  memcpy(srcArtMethodPtr, backupArtMethod, artMethodSizeV1);
  delete[] reinterpret_cast<int8_t*>(backupArtMethod); // 还原时卸载
// TODO: 这里需要修改......
  logv("methodRestore: Success !");
  clear_exception(env);

  return srcArtMethodAddr;
}

//static void set_field_accFlags(JNIEnv* env, jobject fields[]) {
//    if (fields == nullptr) {
//        return;
//    }
//    size_t count = sizeof(fields) / sizeof(jfieldID);
//    if (count <= 0) {
//        return;
//    }
//    for (int i = 0; i < count; ++i) {
//        void* artField = env->FromReflectedField(fields[i]);
//        artField
//    }
//}

static FieldHookClassInfo_t fieldHookClassInfo;

long field_hook_impl(JNIEnv* env, jobject srcField, jobject dstField) {
  // art::mirror::ArtField
  void* srcArtField = reinterpret_cast<void*>(env->FromReflectedField(srcField));
  void* dstArtField = reinterpret_cast<void*>(env->FromReflectedField(dstField));
  int* backupArtField = new int[fieldHookClassInfo.fieldSize];

  memcpy(backupArtField, srcArtField, fieldHookClassInfo.fieldSize);
  memcpy(srcArtField, dstArtField, fieldHookClassInfo.fieldSize);

  logv("hook_field: Success !");
  clear_exception(env);

  return reinterpret_cast<jlong>(backupArtField); // TODO: ing 记得 delete[] 掉
}

static jlong field_restore(JNIEnv* env, jobject srcArtField, jlong backupSrcArtFieldPtr) {
//  void* backupSrcArtField = reinterpret_cast<void*>(backupSrcArtFieldPtr);
//  void* srcArtMethod = reinterpret_cast<void*>(env->FromReflectedMethod(srcArtField));
//  memcpy(srcArtMethod, backupArtMethod, methodHookClassInfo.methodSize);
//  delete[] reinterpret_cast<int8_t*>(backupSrcArtFieldPtr); // 还原时卸载
//
//  logv("methodRestore: Success !");
//  clear_exception(env);
//  return srcMethod;
  return -1L; // TODO: 有待搞定......
}

long class_hook_impl(JNIEnv* env, jstring clazzName) {
  jboolean isCopy;
  const char* kClassName = env->GetStringUTFChars(clazzName, &isCopy);
  std::string kClassNameStr(kClassName);
  std::replace(kClassNameStr.begin(), kClassNameStr.end(), '.', '/');
  logd("hookClass, className=%s", kClassNameStr.c_str());
  jclass kClass = env->FindClass(kClassNameStr.c_str());
  env->ReleaseStringUTFChars(clazzName, kClassName);

  clear_exception(env);

  return reinterpret_cast<jlong>(kClass);
}

void set_cur_thread_impl(JNIEnv* env, long threadAddr) {
  cur_thread = reinterpret_cast<void*>(threadAddr);
  clear_exception(env);
  logi("set_cur_thread, cur_thread=%p", cur_thread);
}

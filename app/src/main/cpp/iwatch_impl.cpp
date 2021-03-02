//
// Created by 葛祥林 on 1/8/21.
//

#include "iwatch_impl.h"
#include "ArtMethodHook.h"
#include "ArtRestore.h"
#include "ArtHookField.h"

#include "common/constants.h"

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

// dex diff

// iwatch 思考的问题点 - done
// 1. 需要加锁 + 条件变量来保护？？？？？？难做到......
// 1.1 hook替换时，原始函数刚好执行到这里；需要互斥
// 1.2 正在恢复时，业务函数正好执行到这里，反之情况也一样；
// 答案：
// 方案1: 有个办法是给这块儿内存(原始或替换后的方法地址)加上锁，谁访问都要互斥；不好，影响性能，而且没必要.
// 方案2: 如果啥都不修改，是否表示当前函数地址在某一时刻的调用，是原子的？？？？？？或者更精确一点是：获取原始函数地址的操作是原子的.
//       经过与fun哥讨论，基于一个原理是：汇编的一条指令是原子的，超过一条指令就不是原子的，函数call指令，在取函数地址时，从内存取
//       到寄存器时，是一条汇编指令，也就是原子的，那么我这里hook替换时，是把新的函数地址从寄存器替换到存储原始函数的地址的内存中，
//       也是一条指令，都具有原子性，因此不存在互斥竞态问题。done!!!!!!

/**
 * 这里仅仅只有一个目的，就是为了计算出不同平台下，每个 art::mirror::ArtMethod 大小，
 * 这里 jmethodID 就是 ArtMethod.
 * 比起 ArtFix，iWatch 方案屏蔽细节、尽量通用，没有适配性。
 */

namespace iwatch {

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
//FromReflectedMethod_t FromReflectedMethod;

// 方案2:
// art/runtime/jni/jni_internal.h
// ArtMethod* FindMethodJNI(const ScopedObjectAccess& soa,
//                         jclass java_class,
//                         const char* name,
//                         const char* sig,
//                         bool is_static);
// 好处是：不是class文件，是全局函数的非类文件
//FindMethodJNI_t FindMethodJNI;
// 同时使用 方案1 和 方案2，哪个生效用哪里，双保险!!!!!!

std::shared_ptr<Elf> elfOp = nullptr;
std::shared_ptr<ArtRestore> artRestore = nullptr;
std::shared_ptr<ArtMethodHook> artMethodHook = nullptr;

int sdkVersion = 0;

void init_impl(JNIEnv* env, int sdkVersionCode, jobject m1, jobject m2) {
  sdkVersion = sdkVersionCode;

  env->GetJavaVM(&vm);

  elfOp = std::make_shared<Elf>();
  artMethodHook = std::make_shared<ArtMethodHook>();
  artRestore = std::make_shared<ArtRestore>();

  // art::mirror::ArtMethod
//  auto artMethod11 = env->FromReflectedMethod(m1);
//  auto artMethod22 = env->FromReflectedMethod(m2);
//  artMethodSize = (size_t) artMethod22 - (size_t) artMethod11;
//  logd("iwatch init artMethodSize, success=%zu, %zu, %zu", artMethodSize,
//       (size_t) artMethod22,
//       (size_t) artMethod11);
  logw("iwatch init, sdkVersion: %d", sdkVersionCode);
  if (sdkVersionCode <= SDK_INT_ANDROID_10) { // <= Android-10(api-29)
    artMethodHook->initArtMethodLessEqual10(env);
  } else { // >= Android-11(api-30)
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
    env->DeleteLocalRef(ArtMethodSizeClass);

//    auto IsIndexId1 = (reinterpret_cast<uintptr_t>(methodid1) % 2) != 0;
//    auto IsIndexId2 = (reinterpret_cast<uintptr_t>(methodid2) % 2) != 0;
//    logi("artMethodSize-2 = %zu, %zu, %zu, %zu, %d, %d", artMethodSize,
//                                                  reinterpret_cast<uintptr_t>(ArtMethodSizeClass),
//                                                  reinterpret_cast<uintptr_t>(methodid1),
//                                                  reinterpret_cast<uintptr_t>(methodid2),
//                                                  IsIndexId1, IsIndexId2);

//    void* context = dlopen_elf("libart.so", RTLD_NOW);
    void* so_addr = elfOp->dlopen_elf("libart.so", RTLD_NOW);
    logi("dlopen_elf: %p", so_addr);
    if (so_addr == nullptr) {
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
    artMethodHook->initArtMethod1(env, elfOp, m1, m2);

    // 方案2
    artMethodHook->initArtMethod2(env, elfOp);

//    dlclose_elf(context); // 释放
    elfOp->dlclose_elf(); // 释放

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
long method_hook_impl(JNIEnv* env, jstring srcClass, jstring srcName,
                      jstring srcSig, jobject srcMethod, jobject dstMethod) {

  jboolean isCopy;
  const char* srcClassStr = env->GetStringUTFChars(srcClass, &isCopy);
  if (srcClassStr == nullptr) {
    return I_ERR;
  }
  std::string _class(srcClassStr);
  std::replace_if(_class.begin(), _class.end(), [](const char& ch) -> bool {
    return '.' == ch;
  }, '/');
  env->ReleaseStringUTFChars(srcClass, srcClassStr);

  auto srcFuncStr = env->GetStringUTFChars(srcName, &isCopy);
  if (srcFuncStr == nullptr) {
    return I_ERR;
  }
  std::string _func(srcFuncStr);
  env->ReleaseStringUTFChars(srcName, srcFuncStr);

  auto srcDescStr = env->GetStringUTFChars(srcSig, &isCopy);
  if (srcDescStr == nullptr) {
    return I_ERR;
  }
  std::string _descriptor(srcDescStr);
  env->ReleaseStringUTFChars(srcSig, srcDescStr);

  if (artRestore->inHooking(_class, _func, _descriptor)) {
    return I_OK; // 已经 hook 了，无需重复
  }

  void* srcArtMethod = nullptr;
  void* dstArtMethod = nullptr;

  if (sdkVersion <= SDK_INT_ANDROID_10) { // <= Android-10
    // art::mirror::ArtMethod
    srcArtMethod = artMethodHook->getArtMethodLessEqual10(env, srcMethod);
    dstArtMethod = artMethodHook->getArtMethodLessEqual10(env, dstMethod);
    logd("method_hook(api<=29), srcArtMethod=%p, dstArtMethod=%p", srcArtMethod, dstArtMethod);
    if (srcArtMethod == nullptr || dstArtMethod == nullptr) {
      loge("method_hook(api<=29), srcArtMethod/dstArtMethod is nullptr !");
      clear_exception(env);
      return I_ERR;
    }
    // 这里有坑，大小不正确...... 在 Android-11 系统中这里的大小获取失败，错误！！！！！！
    // 经过研究 android-11.0.0_r17(http://aosp.opersys.com/) 源代码，class类中的ArtMethod
    // 排布依旧是线性分配的，这里没问题，问题是：从 FromReflectedMethod 这里获取地址是错误的(是个52/53很小的数值，
    // 一看就不是地址).
  } else {
    srcArtMethod = artMethodHook->getArtMethod(env, srcMethod);
    dstArtMethod = artMethodHook->getArtMethod(env, dstMethod);
    logd("method_hook(api>=30), srcArtMethod=%p, dstArtMethod=%p", srcArtMethod, dstArtMethod);
    if (srcArtMethod == nullptr || dstArtMethod == nullptr) {
      loge("method_hook(api>=30), srcArtMethod/dstArtMethod is nullptr !");
      clear_exception(env);
      return I_ERR;
    }
  }

  int8_t* backupArtMethod = nullptr;
  const size_t _artMethodSizeV1 = artMethodHook->getArtMethodSizeV1();
  try {
    backupArtMethod = new int8_t[_artMethodSizeV1];
    // 备份原方法
    memcpy(backupArtMethod, srcArtMethod, _artMethodSizeV1);
    // 替换成新方法
    memcpy(srcArtMethod, dstArtMethod, _artMethodSizeV1);
    logd("method_hook, coppy: artMethodSizeV1=%zu", _artMethodSizeV1);
  } catch (std::exception& e) {
    loge("method_hook copy: eception: %s", e.what());
    clear_exception(env);
    return I_ERR;
  }

  auto backupArtMethodAddr = reinterpret_cast<long>(backupArtMethod);
  auto srcArtMethodAddr = reinterpret_cast<long>(srcArtMethod);
  artRestore->save(_class, _func, _descriptor, backupArtMethodAddr, srcArtMethodAddr);

  logi("methodHook: method_hook Success !");
  clear_exception(env);

  // 返回需要备份的原方法数据内容的地址，注意：这里 new 的对象在 restore 逻辑中才需要回收(delete[])
  return backupArtMethodAddr;
}

/**
 * method1 来自 宿主，使用宿主默认的ClassLoader；
 * method2 来自于patch(补丁)，使用补丁中自定义的DexClassLoader，在执行这个函数(里面需要使用ClassLoader)
 * 之前已经由自定义的DexClassLoader.loadClass加载过一次了，在art虚拟机中已经缓存了 class全路径名@classsLoader，
 * 所以这里可以直接从缓存中取到该class对应的classLoader加载.
 */
long method_hookv2_impl(JNIEnv* env,
                        jstring java_class1, jstring name1, jstring sig1, jboolean is_static1,
                        jstring java_class2, jstring name2, jstring sig2, jboolean is_static2) {

  if (sdkVersion <= SDK_INT_ANDROID_10) { // <= Android-10
    loge("method_hookv2 sdkVersion NOT >= 30: %d", sdkVersion);
    clear_exception(env);
    return I_ERR;
  }

  if (java_class1 == nullptr || java_class2 == nullptr
      || name1 == nullptr || name2 == nullptr
      || sig1 == nullptr || sig2 == nullptr) {

    loge("method_hookv2 input params are nullptr !");
    clear_exception(env);
    return I_ERR;
  }

  jboolean isCopy;
  const char* class1Str = env->GetStringUTFChars(java_class1, &isCopy);
  if (class1Str == nullptr) {
    return I_ERR;
  }
  std::string _class1(class1Str);
  std::replace_if(_class1.begin(), _class1.end(), [](const char& ch)->bool {
    return '.' == ch;
  }, '/');
  env->ReleaseStringUTFChars(java_class1, class1Str);

  const char* funcStr1 = env->GetStringUTFChars(name1, &isCopy);
  if (funcStr1 == nullptr) {
    return I_ERR;
  }
  std::string _func1(funcStr1);

  const char* descriptorStr1 = env->GetStringUTFChars(sig1, &isCopy);
  if (descriptorStr1 == nullptr) {
    return I_ERR;
  }
  std::string _descriptor1(descriptorStr1);

  if (artRestore->inHooking(_class1, _func1, _descriptor1)) {
    return I_OK; // 已经 hook 了，无需重复
  }

  jclass jclass1;
  try {
    // 这里拿到的是宿主的ClassLoader
    jclass1 = env->FindClass(_class1.c_str());
  } catch (std::exception& e) {
    loge("method_hookv2 FindClass-1: %s", e.what());
    clear_exception(env);
    return I_ERR;
  }

  auto artMethod1 = artMethodHook->getArtMethod(env, jclass1, funcStr1, descriptorStr1, is_static1);
  env->DeleteLocalRef(jclass1);

  env->ReleaseStringUTFChars(name1, funcStr1);
  env->ReleaseStringUTFChars(sig1, descriptorStr1);

  logi("method_hookv2, artMethod1=%p", artMethod1);
  if (artMethod1 == nullptr) {
    clear_exception(env);
    return I_ERR;
  }

  const char* class2 = env->GetStringUTFChars(java_class2, &isCopy);
  if (class2 == nullptr) {
    return I_ERR;
  }
  jclass jclass2;
  std::string classStr2(class2);
  std::replace_if(classStr2.begin(), classStr2.end(), [](const char& ch)->bool {
    return '.' == ch;
  }, '/');
  try {
    jclass2 = env->FindClass(classStr2.c_str()); // 这里拿到的是patch中的ClassLoader
  } catch (std::exception& e) {
    loge("method_hookv2 FindClass-2: %s", e.what());
    clear_exception(env);
    return I_ERR;
  }

  const char* name_str2 = env->GetStringUTFChars(name2, &isCopy);
  if (name_str2 == nullptr) {
    return I_ERR;
  }
  const char* sig_str2 = env->GetStringUTFChars(sig2, &isCopy);
  if (sig_str2 == nullptr) {
    return I_ERR;
  }
  auto artMethod2 = artMethodHook->getArtMethod(env, jclass2, name_str2, sig_str2, is_static2);
  env->DeleteLocalRef(jclass2);
  env->ReleaseStringUTFChars(java_class2, class2);
  env->ReleaseStringUTFChars(name2, name_str2);
  env->ReleaseStringUTFChars(sig2, sig_str2);
  logi("method_hookv2, artMethod2=%p", artMethod2);
  if (artMethod2 == nullptr) {
    clear_exception(env);
    return I_ERR;
  }

  int8_t* backupArtMethod = nullptr;
  const size_t _artMethodSizeV2 = artMethodHook->getArtMethodSizeV2();
  try {
    backupArtMethod = new int8_t[_artMethodSizeV2];
    memcpy(backupArtMethod, artMethod1, _artMethodSizeV2);
    memcpy(artMethod1, artMethod2, _artMethodSizeV2);
  } catch (std::exception& e) {
    loge("method_hookv2 copy: eception: %s", e.what());
    clear_exception(env);
    return I_ERR;
  }

  auto backupArtMethodAddr = reinterpret_cast<long>(backupArtMethod);
  auto srcArtMethodAddr = reinterpret_cast<long>(artMethod1);
  artRestore->save(_class1, _func1, _descriptor1, backupArtMethodAddr, srcArtMethodAddr);

  logi("method_hookv2: method_hook Success !");
  clear_exception(env);

  // 返回需要备份的原方法数据内容的地址，注意：这里 new 的对象在 restore 逻辑中才需要回收(delete[])
  return backupArtMethodAddr;
}

void restore_method_impl(JNIEnv* env, jstring className, jstring name, jstring sig) {
  jboolean isCopy;
  const char* classStr = env->GetStringUTFChars(className, &isCopy);
  if (classStr == nullptr) {
    return;
  }
  const char* nameStr = env->GetStringUTFChars(name, &isCopy);
  if (nameStr == nullptr) {
    return;
  }
  const char* sigStr = env->GetStringUTFChars(sig, &isCopy);
  if (sigStr == nullptr) {
    return;
  }

  std::string _class(classStr);
  std::replace_if(_class.begin(), _class.end(), [](const char& ch)->bool {
    return '.' == ch;
  }, '/');
  std::string _method(nameStr);
  std::string _descripor(sigStr); // 必须这样，才是左值
  artRestore->restoreArtMethod(_class, _method, _descripor);

  env->ReleaseStringUTFChars(className, classStr);
  env->ReleaseStringUTFChars(name, nameStr);
  env->ReleaseStringUTFChars(sig, sigStr);

  clear_exception(env);
}

void restore_all_method_impl(JNIEnv* env) {
  artRestore->restoreAllArtMethod();
  clear_exception(env);
}

void set_field_public(JNIEnv* env, jobject field) {
  // art::mirror::ArtField
  if (sdkVersion <= SDK_INT_ANDROID_10) { // <= Android-10 <= SDK_INT_ANDROID_10) { // <= Android-10(api-29)
    void* artField = reinterpret_cast<void*>(env->FromReflectedField(field));
    ArtHookField::addAccessFlagsPublic(artField);
  } else {
    void* artFieldAddr = ArtHookField::getArtField(env, field);
    if (artFieldAddr == nullptr) {
      loge("set_field_public: failure !");
    } else {
      logi("set_field_public, artFieldAddr=%p", artFieldAddr);
      ArtHookField::addAccessFlagsPublic(artFieldAddr);
    }
  }

  iwatch::clear_exception(env);
}

void set_method_public(JNIEnv* env, jobject method) {
  // art::mirror::ArtMethod
  artMethodHook->setAccessPublic(env, method);
  iwatch::clear_exception(env);
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
  return I_ERR;
}

static FieldHookClassInfo_t fieldHookClassInfo;

long class_hook_impl(JNIEnv* env, jstring clazzName) {
  jboolean isCopy;
  const char* kClassName = env->GetStringUTFChars(clazzName, &isCopy);
  if (kClassName == nullptr) {
    return -1L;
  }
  std::string kClassNameStr(kClassName);
  std::replace_if(kClassNameStr.begin(), kClassNameStr.end(), [](const char& ch)->bool {
    return '.' == ch;
  }, '/');
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

size_t getArtMethodSize() {
  return artMethodHook->getArtMethodSize();
}

} // namespace iwatch

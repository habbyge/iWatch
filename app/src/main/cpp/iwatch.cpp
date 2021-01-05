//
// Created by 葛祥林 on 2020/11/24.
//

#include <jni.h>
#include <iostream>
//#include <memory>
#include <thread>

#include "common/log.h"
#include "common/elfop.h"
#include "art/art_method_11.h"
#include "art/ScopedFastNativeObjectAccess.h"


//#include <art/runtime/jni/jni_internal.h>
//#include <exception> C/C++ 的 Exception

// -----------------------------------------------------------------------------------------------
// 查看 art 虚拟机代码技巧：
// 1. Java中的class全名(例如：java/lang/reflect/Method)，对应 Art 中的源文件是：
// art/runtime/native/java_lang_reflect_Method.h
// art/runtime/native/java_lang_reflect_Method.cc
// 即标准的 JNI 接口协议.
// -----------------------------------------------------------------------------------------------

static const char* kClassMethodHook = "com/habbyge/iwatch/MethodHook";

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
// TODO: dex diff 问题

/**
 * 这里仅仅只有一个目的，就是为了计算出不同平台下，每个 art::mirror::ArtMethod 大小，
 * 这里 jmethodID 就是 ArtMethod.
 * 比起 ArtFix，iWatch 方案屏蔽细节、尽量通用，没有适配性。
 */
static struct {
  jmethodID m1;
  jmethodID m2;
  size_t methodSize;
} methodHookClassInfo;
// static methodHookClassInfo_t methodHookClassInfo;

static size_t artMethodSize = 0;
static int sdkVersion = 0;
static void* cur_thread = 0;
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

// ==
//static const char* DecodeMethodId_sym = "_ZN3art3jni12JniIdManager14DecodeMethodIdEP10_jmethodID";
//using DecodeMethodId_t = void* (*)(jmethodID);
//DecodeMethodId_t DecodeMethodId;
//static const char* GetGenericMap_Sym =
//    "_ZN3art3jni12JniIdManager13GetGenericMapINS_9ArtMethodEEERNSt3__16vectorIPT_NS4_9allocatorIS7_EEEEv";

static const char* FromReflectedMethod_Sym =
    "_ZN3art9ArtMethod19FromReflectedMethodERKNS_33ScopedObjectAccessAlreadyRunnableEP8_jobject";
// 真实返回值是：/*art::mirror::ArtMethod_11*/
using FromReflectedMethod_t = void* (*)(const art::ScopedObjectAccessAlreadyRunnable& soa, jobject jlr_method);
FromReflectedMethod_t FromReflectedMethod;


static void init(JNIEnv* env, jclass, jint sdkVersionCode, jobject m1, jobject m2) {
  sdkVersion = sdkVersionCode;

  env->GetJavaVM(&vm);

  // art::mirror::ArtMethod
//  auto artMethod11 = env->FromReflectedMethod(m1);
//  auto artMethod22 = env->FromReflectedMethod(m2);
//  artMethodSize = (size_t) artMethod22 - (size_t) artMethod11;
//  logd("iwatch init artMethodSize, success=%zu, %zu, %zu", artMethodSize,
//       (size_t) artMethod22,
//       (size_t) artMethod11);

  if (sdkVersionCode <= 29) { // <= Android-10
    jclass ArtMethodSizeClass = env->FindClass("com/habbyge/iwatch/ArtMethodSize");
    auto artMethod1 = env->GetStaticMethodID(ArtMethodSizeClass, "func1", "()V");
    auto artMethod2 = env->GetStaticMethodID(ArtMethodSizeClass, "func2", "()V");
    artMethodSize = reinterpret_cast<size_t>(artMethod2) - reinterpret_cast<size_t>(artMethod1);

    // artMethodSize = sizeof(ArtMethod);
    logi("artMethodSize-1 = %d, %zu, %zu, %zu", sdkVersionCode, artMethodSize,
                                                reinterpret_cast<size_t>(artMethod2),
                                                reinterpret_cast<size_t>(artMethod1));
  } else { // >= Android-11
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
    jclass ArtMethodSizeClass = env->FindClass("com/habbyge/iwatch/ArtMethodSize");
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
//          : "_ZN3art9JavaVMExt16AddWeakGlobalRefEPNS_6ThreadENS_6ObjPtrINS_6mirror6ObjectEEE"; // 这个也符合Android-11
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
    FromReflectedMethod = reinterpret_cast<FromReflectedMethod_t>(dlsym_elf(context, FromReflectedMethod_Sym));
    logi("init, FromReflectedMethod=%p", FromReflectedMethod);
    const art::ScopedFastNativeObjectAccess soa(env);
    void* artMethod1 = FromReflectedMethod(soa, m1);
    logi("init, method1=%p, %p", artMethod1, m1);
    void* artMethod2 = FromReflectedMethod(soa, m2);
    // jmethodID, jmethodID, ArtMethod*, ArtMethod*
    logi("init, method2=%p, %p", artMethod2, m2);
    artMethodSize = reinterpret_cast<size_t>(artMethod2) - reinterpret_cast<size_t>(artMethod1);

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
}

static void* getArtMethod(JNIEnv* env, jobject method) {
  const art::ScopedFastNativeObjectAccess soa(env);
  void* artMethod = FromReflectedMethod(soa, method);
  return artMethod;
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
static jlong method_hook(JNIEnv* env, jclass,jobject srcMethod, jobject dstMethod) {
  logi("methodHook: method_hook begin: %zu", artMethodSize);

  void* srcArtMethod = nullptr;
  void* dstArtMethod = nullptr;

  if (sdkVersion <= 29) { // <= Android-10
    // art::mirror::ArtMethod
    srcArtMethod = reinterpret_cast<void*>(env->FromReflectedMethod(srcMethod));
    dstArtMethod = reinterpret_cast<void*>(env->FromReflectedMethod(dstMethod));
    logd("method_hook(api<=29), srcArtMethod=%p, dstArtMethod=%p", srcArtMethod, dstArtMethod);
    // 这里有坑，大小不正确...... 在 Android-11 系统中这里的大小获取失败，错误！！！！！！
    // 经过研究 android-11.0.0_r17(http://aosp.opersys.com/) 源代码，class类中的ArtMethod
    // 排布依旧是线性分配的，这里没问题，问题是：从 FromReflectedMethod 这里获取地址是错误的(是个52/53很小的数值，
    // 一看就不是地址).
  } else {
    srcArtMethod = getArtMethod(env, srcMethod);
    dstArtMethod = getArtMethod(env, dstMethod);
    logd("method_hook(api>=30), srcArtMethod=%p, dstArtMethod=%p", srcArtMethod, dstArtMethod);
  }

  char* backupArtMethod = new char[artMethodSize];
  // 备份原方法
  memcpy(backupArtMethod, srcArtMethod, artMethodSize);
  // 替换成新方法
  memcpy(srcArtMethod, dstArtMethod, artMethodSize);

  logi("methodHook: method_hook Success !");

  // 返回原方法地址
  return reinterpret_cast<jlong>(backupArtMethod);
}

static jobject restore_method(JNIEnv* env, jclass,jobject srcMethod, jlong methodPtr) {
  void* backupArtMethod = reinterpret_cast<void*>(methodPtr);
  void* srcArtMethod = reinterpret_cast<void*>(env->FromReflectedMethod(srcMethod));
  memcpy(srcArtMethod, backupArtMethod, methodHookClassInfo.methodSize);
  delete[] reinterpret_cast<char*>(backupArtMethod); // 还原时卸载

  logv("methodRestore: Success !");

  return srcMethod;
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

/**
 * 这里仅仅只有一个目的，就是为了计算出不同平台下，每个 art::mirror::ArtFiled 大小，
 * 这里 jfieldID 就是 ArtFiled.
 */
typedef struct {
  jfieldID field1;
  jfieldID field2;
  size_t fieldSize;
} fieldHookClassInfo_t;

static fieldHookClassInfo_t fieldHookClassInfo;

static jlong hook_field(JNIEnv* env, jclass, jobject srcField, jobject dstField) {
  // art::mirror::ArtField
  void* srcArtField = reinterpret_cast<void*>(env->FromReflectedField(srcField));
  void* dstArtField = reinterpret_cast<void*>(env->FromReflectedField(dstField));
  int* backupArtField = new int[fieldHookClassInfo.fieldSize];

  memcpy(backupArtField, srcArtField, fieldHookClassInfo.fieldSize);
  memcpy(srcArtField, dstArtField, fieldHookClassInfo.fieldSize);
  logv("hook_field: Success !");
  return reinterpret_cast<jlong>(backupArtField); // 记得 free 掉
}

static jlong hook_class(JNIEnv* env, jclass, jstring clazzName) {
  jboolean isCopy;
  const char* kClassName = env->GetStringUTFChars(clazzName, &isCopy);
  logd("hookClass, className=%s", kClassName);
  jclass kClass = env->FindClass(kClassName);
  env->ReleaseStringUTFChars(clazzName, kClassName);
  return reinterpret_cast<jlong>(kClass);
}

static void set_cur_thread(JNIEnv* env, jclass, jlong threadAddr) {
  cur_thread = reinterpret_cast<void*>(threadAddr);
  logi("set_cur_thread, cur_thread=%p", cur_thread);
}

static JNINativeMethod gMethods[] = {
  {
    "init",
    "(ILjava/lang/reflect/Method;Ljava/lang/reflect/Method;)V",
    (void*) init
  },
  {
    "hookMethod",
    "(Ljava/lang/reflect/Method;Ljava/lang/reflect/Method;)J",
    (void*) method_hook
  },
  {
    "restoreMethod",
    "(Ljava/lang/reflect/Method;J)Ljava/lang/reflect/Method;",
    (void*) restore_method
  },
  {
    "hookField",
    "(Ljava/lang/reflect/Field;Ljava/lang/reflect/Field;)J",
    (void*) hook_field
  },
  {
    "hookClass",
    "(Ljava/lang/String;)J",
    (void*) hook_class
  },
  {
    "setCurThread",
    "(J)V",
    (void*) set_cur_thread
  }
};

extern "C" JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void*) {
  JNIEnv* env = nullptr;
  if (vm->GetEnv((void**) &env, JNI_VERSION_1_4) != JNI_OK) {
    return JNI_FALSE;
  }
  jclass classEvaluateUtil = env->FindClass(kClassMethodHook);
  size_t count = sizeof(gMethods) / sizeof(gMethods[0]);
  if (env->RegisterNatives(classEvaluateUtil, gMethods, count) < 0) {
    return JNI_FALSE;
  }
  return JNI_VERSION_1_4;
}

uint32_t art::mirror::Object::ClassSize(art::PointerSize pointer_size) {
  return 0;
}

//
// Created by 葛祥林 on 2020/11/24.
//

#include <jni.h>
#include "util/log.h"
//#include <memory>
#include "art/art_method_11.h"
//#include <exception> C/C++ 的 Exception

static const char* kClassMethodHook = "com/habbyge/iwatch/MethodHook";

// FIXME: 方案限制：inline 函数 fix 会失败.
//  这里需要考虑，对 inline 函数的影响，内联函数是否有 Method 对象，是否能够被成功替换 ？
//  需要研究：直接 ArtMethod 整体替换的方案，是否能够解决 inline 问题 ？
//  这篇文章对 inline 的影响有详尽研究(同时也收录到了我的有道云笔记中了)：
//  https://cloud.tencent.com/developer/article/1005604
//  内联的源代码位置：art/compiler/optimizing/inliner.cc 中的 HInliner::Run()函数中.
//  实际上，针对 "数据统计补丁" 来说，一般很少有机会在 inline 方法中插桩，即使需要，也可以通过其他手段突破.
//  因此，作为 局限性之一的 inline fix 失败，实际上对 数据统计补丁来说，影响并不大，凡是并不仅能尽善尽美，接收缺陷.
//  我研究了一阵子，就replace inline函数来说，从native层来说，几乎无解。。。
//  如果在 Java层给每一个函数插桩一个try-catch，来阻止编译期的inline，实际上是非常糟糕的做法。。不能被接受
//  实际上所有的方案，都不能尽善尽美，做不到 inline 的话，统计上报需求来了后，我们自己评估下，如果需要在可能被
//  inline 的方法中插桩的话，我们就通过发版本方式来统计。。其实也还好.
//  我 review 了下，我之前的那么多统计上报代码，在可能被 inline 的地方添加上报的可能性几乎么有。

// TODO: Android-11适配问题
// TODO: dex diff 问题

/**
 * 这里仅仅只有一个目的，就是为了计算出不同平台下，每个 art::mirror::ArtMethod 大小，
 * 这里 jmethodID 就是 ArtMethod.
 * 比起 ArtFix， iWatch方案屏蔽细节、尽量通用，没有适配性。
 */
static struct {
  jmethodID m1;
  jmethodID m2;
  size_t methodSize;
} methodHookClassInfo;
//static methodHookClassInfo_t methodHookClassInfo;

static size_t artMethodSize = 0;
static int sdkVersion = 0;

static void init(JNIEnv* env, jclass, jint sdkVersionCode, jobject m1, jobject m2) {
  sdkVersion = sdkVersionCode;

  // art::mirror::ArtMethod
  auto artMethod11 = env->FromReflectedMethod(m1);
  auto artMethod22 = env->FromReflectedMethod(m2);
  artMethodSize = (size_t) artMethod22 - (size_t) artMethod11;
  logd("iwatch init artMethodSize, success=%zu, %zu, %zu", artMethodSize,
       (size_t) artMethod22,
       (size_t) artMethod11);

  if (sdkVersionCode <= 29) { // <= Android-10
    jclass ArtMethodSizeClass = env->FindClass("com/habbyge/iwatch/ArtMethodSize");
    auto artMethod1 = env->GetStaticMethodID(ArtMethodSizeClass, "func1", "()V");
    auto artMethod2 = env->GetStaticMethodID(ArtMethodSizeClass, "func2", "()V");
    artMethodSize = reinterpret_cast<size_t>(artMethod2) - reinterpret_cast<size_t>(artMethod1);

    // artMethodSize = sizeof(ArtMethod);
    logi("artMethodSize = %d, %zu, %zu, %zu", sdkVersionCode, artMethodSize,
         reinterpret_cast<size_t>(artMethod2),
         reinterpret_cast<size_t>(artMethod1));
  } else { // >= Android-11
    loge("iwatch init, sdk >= API-30(Android-11): %d", sdkVersionCode);

    // TODO: >= Android-11的机器有待适配
//    jclass ArtMethodSizeClass = env->FindClass("com/habbyge/iwatch/ArtMethodSize");
//    auto artMethod1 = (void**) env->GetStaticMethodID(ArtMethodSizeClass, "func1", "()V");
//    auto artMethod2 = (void**) env->GetStaticMethodID(ArtMethodSizeClass, "func2", "()V");
//    artMethodSize = reinterpret_cast<size_t>(artMethod2) - reinterpret_cast<size_t>(artMethod1);
//    // artMethodSize = sizeof(ArtMethod);
//    logi("artMethodSize = %zu, %zu, %zu", artMethodSize,
//                                          reinterpret_cast<size_t>(artMethod2),
//                                          reinterpret_cast<size_t>(artMethod1));

    artMethodSize = sizeof(art::mirror::art_method_11);
    logi("artMethodSize = %zu", artMethodSize);
  }
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
static jlong method_hook(JNIEnv* env, jclass, jobject srcMethod, jobject dstMethod) {
  logi("methodHook: method_hook begin: %zu", artMethodSize);

  // art::mirror::ArtMethod
  void* srcArtMethod = reinterpret_cast<void*>(env->FromReflectedMethod(srcMethod));
  void* dstArtMethod = reinterpret_cast<void*>(env->FromReflectedMethod(dstMethod));
  logd("method_hook, srcArtMethod=%zu, dstArtMethod=%zu", (size_t)srcArtMethod, (size_t)dstArtMethod);

  // TODO: 这里有坑，大小不正确...... 在 Android-11 系统中这里的大小获取失败，错误！！！！！！
  //  经过研究 android-11.0.0_r17(http://aosp.opersys.com/) 源代码，class类中的ArtMethod
  //  排布依旧是线性分配的，这里没问题，问题是：从 FromReflectedMethod 这里获取地址是错误的(是个52/53很小的数值，
  //  一看就不是地址).
  int* backupArtMethod = new int[artMethodSize];
  // 备份原方法
  memcpy(backupArtMethod, srcArtMethod, artMethodSize);
  // 替换成新方法
  memcpy(srcArtMethod, dstArtMethod, artMethodSize);

  logi("methodHook: method_hook Success !");

  // 返回原方法地址
  return reinterpret_cast<jlong>(backupArtMethod);
}

static jobject restore_method(JNIEnv* env, jclass, jobject srcMethod, jlong methodPtr) {
  void* backupArtMethod = reinterpret_cast<void*>(methodPtr);
  void* srcArtMethod = reinterpret_cast<void*>(env->FromReflectedMethod(srcMethod));
  memcpy(srcArtMethod, backupArtMethod, methodHookClassInfo.methodSize);
  delete[] reinterpret_cast<int*>(backupArtMethod); // 还原时卸载

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

static jlong hook_field(JNIEnv* env, jclass, jobject
                        srcField, jobject dstField) {

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

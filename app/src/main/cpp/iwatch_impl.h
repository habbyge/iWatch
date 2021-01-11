//
// Created by 葛祥林 on 1/8/21.
//

#ifndef IWATCH_IWATCH_H
#define IWATCH_IWATCH_H

#include <jni.h>
//#include <memory>
#include <thread>
#include <exception>
#include <string>
#include <algorithm>
#include <stddef.h>

#include "common/log.h"
#include "common/elfop.h"
#include "art/art_method_11.h"
#include "art/ScopedFastNativeObjectAccess.h"
#include "art/scoped_thread_state_change.h"

static const char* kClassMethodHook = "com/habbyge/iwatch/MethodHook";
static const char* computeArtMethodSize_ClassName = "com/habbyge/iwatch/ArtMethodSize";

static const char* FromReflectedMethod_Sym =
    "_ZN3art9ArtMethod19FromReflectedMethodERKNS_33ScopedObjectAccessAlreadyRunnableEP8_jobject";
// 真实返回值是：/*art::mirror::ArtMethod_11*/
using FromReflectedMethod_t = void* (*)(const art::ScopedObjectAccessAlreadyRunnable& soa, jobject jlr_method);

static const char* FindMethodJNI_Sym = "_ZN3art13FindMethodJNIERKNS_18ScopedObjectAccessEP7_jclassPKcS6_b";
// 真实返回值是：/*art::mirror::ArtMethod_11*/
using FindMethodJNI_t = void* (*)(const art::ScopedObjectAccess& soa,
                                  jclass java_class,
                                  const char* name,
                                  const char* sig,
                                  bool is_static);

/**
 * 这里仅仅只有一个目的，就是为了计算出不同平台下，每个 art::mirror::ArtMethod 大小，
 * 这里 jmethodID 就是 ArtMethod.
 * 比起 ArtFix，iWatch 方案屏蔽细节、尽量通用，没有适配性。
 */
typedef struct {
  jmethodID m1;
  jmethodID m2;
  size_t methodSize;
} MethodHookClassInfo_t;

/**
 * 这里仅仅只有一个目的，就是为了计算出不同平台下，每个 art::mirror::ArtFiled 大小，
 * 这里 jfieldID 就是 ArtFiled.
 */
typedef struct {
  jfieldID field1;
  jfieldID field2;
  size_t fieldSize;
} FieldHookClassInfo_t;

#ifdef __cplusplus
extern "C" {
#endif

/**
 * 这里在 C/C++(jni/naitve) 层发生的 exception，如果被jni方法(在java层声明的Native方法)直接或间接调用，
 * 即使是 catch 住了，也需要 clear 掉该 exception，否则会继续向上炮 exception 到 Java 层，这里需要注意.
 */
static inline void clear_exception(JNIEnv* env) {
  if (env->ExceptionCheck()) {
    env->ExceptionClear(); // 清除异常，避免 jni 函数的异常抛到 Java 层
  }
}

void init_impl(JNIEnv* env, int sdkVersionCode, jobject m1, jobject m2);

long method_hook_impl(JNIEnv* env, jstring srcClass, jstring srcName, jstring srcSig,
                      jobject srcMethod, jobject dstMethod);

long method_hookv2_impl(JNIEnv* env,
                        jstring java_class1, jstring name1, jstring sig1, jboolean is_static1,
                        jstring java_class2, jstring name2, jstring sig2, jboolean is_static2);

/**
 * 恢复原始方法: ArtMethod
 * @return 返回原始 ArtMethod 地址
 */
void restore_method_impl(JNIEnv* env, jstring className, jstring name, jstring sig);

long field_hook_impl(JNIEnv* env, jobject srcField, jobject dstField);
long class_hook_impl(JNIEnv* env, jstring clazzName);

void set_cur_thread_impl(JNIEnv* env, long threadAddr);

#ifdef __cplusplus
}
#endif

#endif //IWATCH_IWATCH_H

//
// Created by habbyge on 1/26/21.
//

#pragma once

#ifndef IWATCH_ARTMETHODHOOK_H
#define IWATCH_ARTMETHODHOOK_H

#include <jni.h>
#include <memory>
#include <thread>
#include <exception>
#include <string>
#include <algorithm>
#include <stddef.h>

#include "common/elf_op.h"

#include "common/log.h"
#include "common/elf_op.h"
#include "art/art_method_11.h"
#include "art/ScopedFastNativeObjectAccess.h"
#include "art/scoped_thread_state_change.h"

namespace iwatch {

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

static const char* computeArtMethodSize_ClassName = "com/habbyge/iwatch/ArtMethodSize";

// art/runtime/art_method.h
static const char* FromReflectedMethod_Sym =
    "_ZN3art9ArtMethod19FromReflectedMethodERKNS_33ScopedObjectAccessAlreadyRunnableEP8_jobject";
// 真实返回值是：/*art::mirror::ArtMethod_11*/
using FromReflectedMethod_t = void* (*)(const art::ScopedObjectAccessAlreadyRunnable& soa,
                                        jobject jlr_method);

// art/runtime/jni/jni_internal.h
static const char* FindMethodJNI_Sym = "_ZN3art13FindMethodJNIERKNS_18ScopedObjectAccessEP7_jclassPKcS6_b";
// 真实返回值是：/*art::mirror::ArtMethod_11*/
using FindMethodJNI_t = void* (*)(const art::ScopedObjectAccess& soa,
                                  jclass java_class,
                                  const char* name,
                                  const char* sig,
                                  bool is_static);

class ArtMethodHook final {
public:
  ArtMethodHook() : artMethodSizeV1(-1), artMethodSizeV2(-1) {}

  virtual ~ArtMethodHook() {}

  void initArtMethodLessEqual10(JNIEnv* env);
  void initArtMethod1(JNIEnv* env, const std::shared_ptr<Elf>& elf_op, jobject m1, jobject m2);
  void initArtMethod2(JNIEnv* env, const std::shared_ptr<Elf>& elf_op);

  void* getArtMethodLessEqual10(JNIEnv* env, jobject method);
  void* getArtMethod(JNIEnv* env, jobject method);
  void* getArtMethod(JNIEnv* env, jclass java_class, const char* name, const char* sig, bool is_static);

  void setAccessPublic(JNIEnv* env, jobject method);

  inline size_t getArtMethodSizeV1() const {
    return artMethodSizeV1;
  }

  inline size_t getArtMethodSizeV2() const {
    return artMethodSizeV2;
  }

  inline size_t getArtMethodSize() const {
    return artMethodSizeV1 <= 0 ? artMethodSizeV2 : artMethodSizeV1;
  }

private:
  size_t artMethodSizeV1;
  size_t artMethodSizeV2;

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
};

} // namespace iwatch

#endif //IWATCH_ARTMETHODHOOK_H

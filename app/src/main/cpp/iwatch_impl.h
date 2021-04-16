//
// Created by 葛祥林 on 1/8/21.
//

#pragma once

#ifndef IWATCH_IWATCH_H
#define IWATCH_IWATCH_H

#include <jni.h>
#include <memory>
#include <thread>
#include <exception>
#include <string>
#include <algorithm>
#include <stddef.h>

#include "common/log.h"
#include "common/elf_op.h"
#include "common/constants.h"

#include "runtime.h"

namespace iwatch {

static const char* kClassMethodHook = "com/habbyge/iwatch/MethodHook";

/**
 * 这里在 C/C++(jni/naitve) 层发生的 exception，如果被jni方法(在java层声明的Native方法)直接或间接调用，
 * 即使是 catch 住了，也需要 clear 掉该 exception，否则会继续向上炮 exception 到 Java 层，这里需要注意.
 */
static inline void clear_exception(JNIEnv* env) {
  if (env->ExceptionCheck()) {
    env->ExceptionDescribe(); // 打印异常堆栈信息
    env->ExceptionClear();    // 清除异常，避免 jni 函数的异常抛到 Java 层
  }
  // 如上代码进行处理后，App并不会直接崩溃了，并且在Logcat中会看到对应的异常日志，这里面到了做了哪些操作呢 ?
  // Native 提供了 ExceptionOccurred 和 ExceptionCheck 方法来检测是否有异常发生:
  // 前者返回的是 jthrowable 类型，后者返回的是 jboolean 类型。
  // 如果有异常，会通过 ExceptionDescribe 方法来打印异常信息，方便我们在 LogCat 中看到对应的信息。
  // 而 ExceptionClear 方法则是关键的不会让应用直接崩溃的方法，类似于 Java 的 catch 捕获异常处理，它会消除这次异常。
}

void init_impl(JNIEnv* env, int sdkVersionCode, jobject method1, jobject method2);

long method_hook_impl(JNIEnv* env, jstring srcClass, jstring srcName, jstring srcSig,
                      jobject srcMethod, jobject dstMethod);

long method_hookv2_impl(JNIEnv* env, jobject method1, jobject method2,
                        jstring java_class1, jstring name1, jstring sig1, jboolean is_static1,
                        jstring java_class2, jstring name2, jstring sig2, jboolean is_static2);

/**
 * 恢复原始方法: ArtMethod
 * @return 返回原始 ArtMethod 地址
 */
void restore_method_impl(JNIEnv* env, jstring className, jstring name, jstring sig);
void restore_all_method_impl(JNIEnv* env);

void set_field_public(JNIEnv* env, jobject field, jclass srcClas, jstring name, jstring sig, jboolean isStatic);
void set_method_public(JNIEnv* env, jobject method);

long class_hook_impl(JNIEnv* env, jstring clazzName);

void set_cur_thread_impl(JNIEnv* env, long threadAddr);

size_t getArtMethodSize();

uint64_t get_tid();

template<bool kEnableIndexIds>
static inline bool isIndexId(jfieldID fid) {
  return kEnableIndexIds && ((reinterpret_cast<uintptr_t>(fid) % 2) != 0);
}

template<bool kEnableIndexIds>
static inline bool isIndexId(jmethodID mid) {
  return kEnableIndexIds && ((reinterpret_cast<uintptr_t>(mid) % 2) != 0);
}

} // namespace iwatch

#endif //IWATCH_IWATCH_H

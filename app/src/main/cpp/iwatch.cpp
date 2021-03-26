//
// Created by 葛祥林 on 2020/11/24.
//

/**
 * 目前无需考虑使用多线程<thread>来hook，评估hook不耗时，另外优先考虑在当前线程中进行hook，避免地址偏差.
 */

#include "iwatch_impl.h"

static void init(JNIEnv* env, jclass, jint sdkVersionCode, jobject m1, jobject m2) {
  iwatch::init_impl(env, sdkVersionCode, m1, m2);
}

static jlong method_hook(JNIEnv* env, jclass, jstring srcClass,
                         jstring srcName, jstring  srcSig,
                         jobject srcMethod, jobject dstMethod) {

  return iwatch::method_hook_impl(env, srcClass, srcName, srcSig, srcMethod, dstMethod);
}

static jlong method_hookv2(JNIEnv* env, jclass, jobject method1, jobject method2,
                           jstring java_class1, jstring name1, jstring sig1, jboolean is_static1,
                           jstring java_class2, jstring name2, jstring sig2, jboolean is_static2) {

  return iwatch::method_hookv2_impl(env, method1, method2,
                                    java_class1, name1, sig1, is_static1,
                                    java_class2, name2, sig2, is_static2);
}

static void restore_method(JNIEnv* env, jclass, jstring className, jstring name, jstring sig) {
  iwatch::restore_method_impl(env, className, name, sig);
}

static void restore_all_method(JNIEnv* env, jclass) {
  iwatch::restore_all_method_impl(env);
}

static void set_field_public(JNIEnv* env, jclass, jobject field,
                             jclass srcClass, jstring name, jstring sig,
                             jboolean isStatic) {

  iwatch::set_field_public(env, field, srcClass, name, sig, isStatic);
}

static void set_method_public(JNIEnv* env, jclass, jobject method) {
  iwatch::set_method_public(env, method);
}

static jlong class_hook(JNIEnv* env, jclass, jstring clazzName) {
  return iwatch::class_hook_impl(env, clazzName);
}

static void set_cur_thread(JNIEnv* env, jclass, jlong threadAddr) {
  iwatch::set_cur_thread_impl(env, threadAddr);
}

static JNINativeMethod gMethods[] = {
  {
    "init",
    "(ILjava/lang/reflect/Method;Ljava/lang/reflect/Method;)V",
    (void*) init
  },
  {
    "hookMethod",
    "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/reflect/Method;Ljava/lang/reflect/Method;)J",
    (void*) method_hook
  },
  {
      "hookMethod",
      "(Ljava/lang/reflect/Method;Ljava/lang/reflect/Method;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;ZLjava/lang/String;Ljava/lang/String;Ljava/lang/String;Z)J",
      (void*) method_hookv2
  },
  {
    "unhookMethod",
    "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V",
    (void*) restore_method
  },
  {
    "unhookAllMethod",
    "()V",
    (void*) restore_all_method
  },
  {
    "setFieldAccessPublic",
    "(Ljava/lang/reflect/Field;Ljava/lang/Class;Ljava/lang/String;Ljava/lang/String;Z)V",
    (void*) set_field_public
  },
  {
      "setMethodAccessPublic",
      "(Ljava/lang/reflect/Method;)V",
      (void*) set_method_public
  },
  {
    "hookClass",
    "(Ljava/lang/String;)J",
    (void*) class_hook
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
  jclass classEvaluateUtil = env->FindClass(iwatch::kClassMethodHook);
  size_t count = sizeof(gMethods) / sizeof(gMethods[0]);
  if (env->RegisterNatives(classEvaluateUtil, gMethods, count) < 0) {
    return JNI_FALSE;
  }
  return JNI_VERSION_1_4;
}

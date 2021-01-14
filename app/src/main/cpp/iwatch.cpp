//
// Created by 葛祥林 on 2020/11/24.
//

#include "iwatch_impl.h"

static void init(JNIEnv* env, jclass, jint sdkVersionCode, jobject m1, jobject m2) {
  init_impl(env, sdkVersionCode, m1, m2);
}

static jlong method_hook(JNIEnv* env, jclass, jstring srcClass,
                         jstring srcName, jstring  srcSig,
                         jobject srcMethod, jobject dstMethod) {

  return method_hook_impl(env, srcClass, srcName, srcSig, srcMethod, dstMethod);
}

static jlong method_hookv2(JNIEnv* env, jclass,
                           jstring java_class1, jstring name1, jstring sig1, jboolean is_static1,
                           jstring java_class2, jstring name2, jstring sig2, jboolean is_static2) {

  return method_hookv2_impl(env, java_class1, name1, sig1, is_static1, java_class2, name2, sig2, is_static2);
}

static void restore_method(JNIEnv* env, jclass, jstring className, jstring name, jstring sig) {
  restore_method_impl(env, className, name, sig);
}

static void restore_all_method(JNIEnv* env, jclass) {
  restore_all_method_impl(env);
}

static jlong field_hook(JNIEnv* env, jclass, jobject srcField, jobject dstField) {
  return field_hook_impl(env, srcField, dstField);
}

static jlong class_hook(JNIEnv* env, jclass, jstring clazzName) {
  return class_hook_impl(env, clazzName);
}

static void set_cur_thread(JNIEnv* env, jclass, jlong threadAddr) {
  set_cur_thread_impl(env, threadAddr);
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
      "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;ZLjava/lang/String;Ljava/lang/String;Ljava/lang/String;Z)J",
      (void*) method_hookv2
  },
  {
    "unhookMethod",
    "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V",
    (void*) restore_method
  },
  {
    "unhookAllmethod",
    "()V",
    (void*) restore_all_method
  },
  {
    "hookField",
    "(Ljava/lang/reflect/Field;Ljava/lang/reflect/Field;)J",
    (void*) field_hook
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
  jclass classEvaluateUtil = env->FindClass(kClassMethodHook);
  size_t count = sizeof(gMethods) / sizeof(gMethods[0]);
  if (env->RegisterNatives(classEvaluateUtil, gMethods, count) < 0) {
    return JNI_FALSE;
  }
  return JNI_VERSION_1_4;
}

//
// Created by 葛祥林 on 2020/11/24.
//

#include "util/log.h"

static const char* kClassMethodHookChar = "com/habbyge/iwatch/MethodHook";

/**
 * 这里仅仅只有一个目的，就是为了计算出不同平台下，每个 art::mirror::ArtMethod 大小，
 * 这里 jmethodID 就是 ArtMethod.
 */
static struct {
    jmethodID m1;
    jmethodID m2;
    size_t methodSize;
} methodHookClassInfo;

static jlong method_hook(JNIEnv* env, jclass,
                         jobject srcMethodObj,
                         jobject dstMethodObj) {

    // art::mirror::ArtMethod
    void* srcMethod = reinterpret_cast<void*>(env->FromReflectedMethod(srcMethodObj));
    void* dstMethod = reinterpret_cast<void*>(env->FromReflectedMethod(dstMethodObj));

    int* backupMethod = new int[methodHookClassInfo.methodSize];
    // 备份原方法
    memcpy(backupMethod, srcMethod, methodHookClassInfo.methodSize);
    // 替换成新方法
    memcpy(srcMethod, dstMethod, methodHookClassInfo.methodSize);

    LOGV("methodHook: Success !");

    // 返回原方法地址
    return reinterpret_cast<jlong>(backupMethod);
}

static jobject restore_method(JNIEnv* env, jclass,
                              jobject srcMethod, jlong methodPtr) {

    LOGV("methodRestore: start !!!");

    void* backupMethod = reinterpret_cast<void*>(methodPtr);
    void* artMethodSrc = reinterpret_cast<void*>(env->FromReflectedMethod(srcMethod));
    memcpy(artMethodSrc, backupMethod, methodHookClassInfo.methodSize);
    delete[] reinterpret_cast<int*>(backupMethod);

    LOGV("methodRestore: Success !");

    return srcMethod;
}

/**
 * 这里仅仅只有一个目的，就是为了计算出不同平台下，每个 art::mirror::ArtFiled 大小，
 * 这里 jfieldID 就是 ArtFiled.
 */
static struct {
    jfieldID field1;
    jfieldID field2;
    size_t fieldSize;
} fieldHookClassInfo;

static jlong hook_field(JNIEnv* env, jclass,
                        jobject srcFieldObj, jobject dstFieldObj) {

    // art::mirror::ArtField
    void* srcField = reinterpret_cast<void*>(env->FromReflectedField(srcFieldObj));
    void* dstField = reinterpret_cast<void*>(env->FromReflectedField(dstFieldObj));
    int* backupField = new int[fieldHookClassInfo.fieldSize];
    memcpy(backupField, srcField, fieldHookClassInfo.fieldSize);
    memcpy(srcField, dstField, fieldHookClassInfo.fieldSize);
    LOGV("hook_field: Success !");
    return reinterpret_cast<jlong>(backupField);
}

static JNINativeMethod gMethods[] = {
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
    }
};

extern "C" JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void*) {
    JNIEnv* env = nullptr;
    if (vm->GetEnv((void**) &env, JNI_VERSION_1_4) != JNI_OK) {
        return JNI_FALSE;
    }
    jclass classEvaluateUtil = env->FindClass(kClassMethodHookChar);
    if (env->RegisterNatives(classEvaluateUtil, gMethods,
                             sizeof(gMethods) / sizeof(gMethods[0])) < 0) {
        return JNI_FALSE;
    }
    methodHookClassInfo.m1 = env->GetStaticMethodID(classEvaluateUtil, "m1", "()V");
    methodHookClassInfo.m2 = env->GetStaticMethodID(classEvaluateUtil, "m2", "()V");
    methodHookClassInfo.methodSize =
            reinterpret_cast<size_t>(methodHookClassInfo.m2) -
            reinterpret_cast<size_t>(methodHookClassInfo.m1);

    fieldHookClassInfo.field1 = env->GetStaticFieldID(classEvaluateUtil,
                                                      "field1",
                                                      "Ljava/lang/Object;");
    fieldHookClassInfo.field2 = env->GetStaticFieldID(classEvaluateUtil,
                                                      "field2",
                                                      "Ljava/lang/Object;");
    fieldHookClassInfo.fieldSize =
            reinterpret_cast<size_t>(fieldHookClassInfo.field2) -
            reinterpret_cast<size_t>(fieldHookClassInfo.field1);

    return JNI_VERSION_1_4;
}

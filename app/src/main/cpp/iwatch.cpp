//
// Created by 葛祥林 on 2020/11/24.
//

#include "util/log.h"

static const char* kClassMethodHookChar = "com/habbyge/iwatch/MethodHook";

// FIXME: 这里需要考虑，对 inline 函数的影响，内联函数是否有 Method 对象，是否能够被成功替换 ？
//  需要研究：直接 ArtMethod 整体替换的方案，是否能够解决 inline 问题 ？
//  我的理解是：即使目标方法 inline 了，但是 ArMethod 对象还存在，只是其中包括的汇编代码段被直接拷贝到了其函数调
//  用处. 这里可能会失败......
//  经源代码 review + 研究 + 测试用例验证，这里 inline 不会对 ArtMethod 有影响，但是，我们在实际使用过程中，
//  仍旧需要对可能被 inline 的方法，尽量避免fix，以免失效不起作用.
//  这篇文章对 inline 的影响有详尽研究(同时也收录到了我的有道云笔记中了)：
//  https://cloud.tencent.com/developer/article/1005604

/**
 * 这里仅仅只有一个目的，就是为了计算出不同平台下，每个 art::mirror::ArtMethod 大小，
 * 这里 jmethodID 就是 ArtMethod.
 * 比起 ArtFix， iWatch方案屏蔽细节、尽量通用，没有适配性。
 */
typedef struct {
    jmethodID m1;
    jmethodID m2;
    size_t methodSize;
} methodHookClassInfo_t;

static methodHookClassInfo_t methodHookClassInfo;

/**
 * 采用整体替换方法结构(art::mirror::ArtMethod)，忽略底层实现，从而解决兼容稳定性问题，
 * 比AndFix稳定可靠.
 * 旧的方案ArtMethod中的
 */
static jlong method_hook(JNIEnv* env, jclass, jobject srcMethod, jobject dstMethod) {
    // art::mirror::ArtMethod
    void* srcArtMethod = reinterpret_cast<void*>(env->FromReflectedMethod(srcMethod));
    void* dstArtMethod = reinterpret_cast<void*>(env->FromReflectedMethod(dstMethod));

    int* backupArtMethod = new int[methodHookClassInfo.methodSize];
    // 备份原方法
    memcpy(backupArtMethod, srcArtMethod, methodHookClassInfo.methodSize);
    // 替换成新方法
    memcpy(srcArtMethod, dstArtMethod, methodHookClassInfo.methodSize);

    LOGI("methodHook: Success !");

    // 返回原方法地址
    return reinterpret_cast<jlong>(backupArtMethod);
}

static jobject restore_method(JNIEnv* env, jclass, jobject srcMethod, jlong methodPtr) {
    void* backupArtMethod = reinterpret_cast<void*>(methodPtr);
    void* srcArtMethod = reinterpret_cast<void*>(env->FromReflectedMethod(srcMethod));
    memcpy(srcArtMethod, backupArtMethod, methodHookClassInfo.methodSize);
    delete[] reinterpret_cast<int*>(backupArtMethod);

    LOGV("methodRestore: Success !");

    return srcMethod;
}

/*static void set_field_accFlags(JNIEnv* env, jobject fields[]) {
    if (fields == nullptr) {
        return;
    }
    size_t count = sizeof(fields) / sizeof(jfieldID);
    if (count <= 0) {
        return;
    }
    for (int i = 0; i < count; ++i) {
        void* artField = env->FromReflectedField(fields[i]);
        artField
    }
}*/

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
    LOGV("hook_field: Success !");
    return reinterpret_cast<jlong>(backupArtField);
}

static jlong hook_class(JNIEnv* env, jclass, jstring clazzName) {
    jboolean isCopy;
    const char* className = env->GetStringUTFChars(clazzName, &isCopy);
    LOGD("hookClass, className=%s", className);
    jclass jclazz = env->FindClass(className);
    env->ReleaseStringUTFChars(clazzName, className);
    return reinterpret_cast<jlong>(jclazz);
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
    jclass classEvaluateUtil = env->FindClass(kClassMethodHookChar);
    size_t count = sizeof(gMethods) / sizeof(gMethods[0]);
    if (env->RegisterNatives(classEvaluateUtil, gMethods, count) < 0) {
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

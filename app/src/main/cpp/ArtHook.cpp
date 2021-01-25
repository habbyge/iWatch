//
// Created by habbyge on 1/26/21.
//

#include "ArtHook.h"

namespace iwatch {

void init(JNIEnv* env, jobject m1, jobject m2) {
  // TODO:
}

long method_hook_impl(JNIEnv* env, jstring srcClass, jstring srcName, jstring srcSig,
                      jobject srcMethod, jobject dstMethod) {
  // TODO:
  return -1L;
}

long method_hookv2_impl(JNIEnv* env,
                        jstring java_class1, jstring name1, jstring sig1, jboolean is_static1,
                        jstring java_class2, jstring name2, jstring sig2, jboolean is_static2) {
// TODO:
  return -1L;
}

} // namespace iwatch

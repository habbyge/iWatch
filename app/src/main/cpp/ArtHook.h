//
// Created by habbyge on 1/26/21.
//

#ifndef IWATCH_ARTHOOK_H
#define IWATCH_ARTHOOK_H

#include <jni.h>
#include <memory>
#include <thread>
#include <exception>
#include <string>
#include <algorithm>
#include <stddef.h>

#include "common/log.h"
//#include "common/elfop.h"
#include "common/elf_op.h"
#include "art/art_method_11.h"
#include "art/ScopedFastNativeObjectAccess.h"
#include "art/scoped_thread_state_change.h"

namespace iwatch {

class ArtHook {
public:
  void init(JNIEnv* env, jobject m1, jobject m2);

  long method_hook_impl(JNIEnv* env, jstring srcClass, jstring srcName, jstring srcSig,
                        jobject srcMethod, jobject dstMethod);

  long method_hookv2_impl(JNIEnv* env,
                          jstring java_class1, jstring name1, jstring sig1, jboolean is_static1,
                          jstring java_class2, jstring name2, jstring sig2, jboolean is_static2);
};

} // namespace iwatch

#endif //IWATCH_ARTHOOK_H

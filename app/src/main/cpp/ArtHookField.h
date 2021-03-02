//
// Created by 葛祥林 on 1/26/21.
//

#pragma once

#ifndef IWATCH_ARTHOOKFIELD_H
#define IWATCH_ARTHOOKFIELD_H

#include <stdint.h>
#include <jni.h>

namespace iwatch {

static constexpr uint32_t kAccPublic =       0x0001;  // class, field, method, ic
static constexpr uint32_t kAccPrivate =      0x0002;  // field, method, ic
static constexpr uint32_t kAccProtected =    0x0004;  // field, method, ic
                                                      // ......
static constexpr uint32_t kAccJavaFlagsMask = 0xffff; // bits set from Java sources (low 16)

/**
 * access_flags_ 在 ArtField 中的偏移量满足：android-5.0 ~ android-11.0
 * 主要实现Class中字段访问权限的改变，private -> public
 */
class ArtHookField final {
public:
  static void* getArtField(JNIEnv* env, jobject field);
  static void* getArtField(JNIEnv* env, jclass java_class, const char* name, const char* sig, bool is_static);

  inline static void addAccessFlagsPublic(void* artField) {
    uint32_t* access_flags_ptr = reinterpret_cast<uint32_t*>(artField) + 1;
    *access_flags_ptr = (*access_flags_ptr) & (~kAccPrivate) | kAccPublic;
  }

private:
  uint32_t reference_;
  uint32_t access_flags_ = 0; // android5.0 ~android11 field 的访问权限字段偏移量都是4byte处
  // ...... 不关注
};

} // namespace iwatch

#endif //IWATCH_ARTHOOKFIELD_H

//
// Created by 葛祥林 on 1/26/21.
//

#pragma once

#ifndef IWATCH_ARTHOOKFIELD_H
#define IWATCH_ARTHOOKFIELD_H

#include <stdint.h>
#include <jni.h>
#include <memory>
#include "art/scoped_thread_state_change.h"
#include "common/elf_op.h"

namespace iwatch {

static constexpr uint32_t kAccPublic =       0x0001;  // class, field, method, ic
static constexpr uint32_t kAccPrivate =      0x0002;  // field, method, ic
static constexpr uint32_t kAccProtected =    0x0004;  // field, method, ic
                                                      // ......
static constexpr uint32_t kAccJavaFlagsMask = 0xffff; // bits set from Java sources (low 16)

// 适配 >= Android-11
// art/runtime/jni/jni_internal.cc 中的 FindFieldJNI() 函数符号
// ArtField* FindFieldJNI(const ScopedObjectAccess& soa,
//                        jclass jni_class,
//                        const char* name,
//                        const char* sig,
//                        bool is_static)
static const char* FindFieldJNI_Sym = "_ZN3art12FindFieldJNIERKNS_18ScopedObjectAccessEP7_jclassPKcS6_b";
using FindFieldJNI = void* (*) (const art::ScopedObjectAccess& soa,
                                jclass jni_class,
                                const char* name,
                                const char* sig,
                                bool is_static);

/**
 * access_flags_ 在 ArtField 中的偏移量满足：android-5.0 ~ android-11.0
 * 主要实现Class中字段访问权限的改变，private -> public
 */
class ArtHookField final {
public:
  static void initArtField(JNIEnv* env, std::shared_ptr<Elf> elf_op);
  static void* getArtField(JNIEnv* env, jobject field);

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

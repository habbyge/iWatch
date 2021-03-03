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
#include "common/log.h"
#include "common/constants.h"
#include "common/elf_op.h"

namespace iwatch {

// 适配 >= Android-11
// art/runtime/jni/jni_internal.cc 中的 FindFieldJNI() 函数符号
// ArtField* FindFieldJNI(const ScopedObjectAccess& soa,
//                        jclass jni_class,
//                        const char* name,
//                        const char* sig,
//                        bool is_static)
static const char* FindFieldJNI_Sym = "_ZN3art12FindFieldJNIERKNS_18ScopedObjectAccessEP7_jclassPKcS6_b";
using FindFieldJNI_t = void* (*)(const art::ScopedObjectAccess& soa,
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
  ArtHookField() : reference_(0), access_flags_(0), FindFieldJNI(nullptr) {}

  ~ArtHookField() {
    FindFieldJNI = nullptr;
  }

  void initArtField(JNIEnv* env, const std::shared_ptr<Elf>& elf_op);
  void* getArtField(JNIEnv* env, jclass jni_class, const char* name, const char* sig, bool isStatic);

  inline static void addAccessFlagsPublic(void* artField) {
    // 从 Android5.0 ~ Android11.0 的版本中，ArtField 中 access_flags_ 字段偏移量都是1
    uint32_t* access_flags_addr = reinterpret_cast<uint32_t*>(artField) + 1;
    uint32_t access_flags_ = *access_flags_addr;
    if ((access_flags_ & kAccSynthetic) == kAccSynthetic) {
      return; // 由于内部类合成的字段(例如：外部类的对象字段)，不能设置其访问权限
    }
    *access_flags_addr = ((*access_flags_addr) & (~kAccPrivate) & (~kAccProtected)) | kAccPublic;
    logw("addAccessFlagsPublic, access_flags_ptr=%p, access_flags=%u -> %u",
         access_flags_addr, access_flags_, *access_flags_addr);
  }

private:
  uint32_t reference_;
  uint32_t access_flags_ = 0; // android5.0 ~android11 field 的访问权限字段偏移量都是4byte处

  FindFieldJNI_t  FindFieldJNI;
  // ...... 不关注
};

} // namespace iwatch

#endif //IWATCH_ARTHOOKFIELD_H

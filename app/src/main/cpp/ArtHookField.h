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

namespace iwatch {

// 适配 >= Android-11
// art/runtime/jni/jni_id_manager.h
// ArtField* DecodeFieldId(jfieldID field)
//static const char* DecodeFieldId_Sym = "_ZN3art3jni12JniIdManager13DecodeFieldIdEP9_jfieldID";
//using DecodeFieldId_t = void* (*)(jfieldID field);

// art/runtime/jni/jni_internal.cc 中的 FindFieldJNI() 函数符号
// ArtField* FindFieldJNI(const ScopedObjectAccess& soa,
//                        jclass jni_class,
//                        const char* name,
//                        const char* sig,
//                        bool is_static)
static const char* FindFieldJNI_Sym = "_ZN3art12FindFieldJNIERKNS_18ScopedObjectAccessEP7_jclassPKcS6_b";
using FindFieldJNI_t = void* (*)(const art::ScopedObjectAccess& soa,
                                 jclass java_class,
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
//    DecodeFieldId = nullptr;
  }

  void initArtField(JNIEnv* env, const std::shared_ptr<Elf>& elf_op);
  /*void* getArtField(JNIEnv* env, jfieldID field);*/
  void* getArtField(JNIEnv* env, jclass jni_class, const char* name, const char* sig, bool isStatic);

  static void addAccessFlagsPublic(void* artField);

private:
  uint32_t reference_;
  uint32_t access_flags_{0}; // android5.0 ~ android11 field 的访问权限字段偏移量都是4byte处

//  DecodeFieldId_t DecodeFieldId; // 主力
  FindFieldJNI_t FindFieldJNI;   // 辅助
  // ...... 不关注
};

} // namespace iwatch

#endif //IWATCH_ARTHOOKFIELD_H

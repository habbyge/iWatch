//
// Created by 葛祥林 on 1/26/21.
//

#pragma once

#ifndef IWATCH_ARTHOOKFIELD_H
#define IWATCH_ARTHOOKFIELD_H

#include <stdint.h>

namespace iwatch {

static constexpr uint32_t kAccPublic =       0x0001;  // class, field, method, ic
static constexpr uint32_t kAccPrivate =      0x0002;  // field, method, ic
static constexpr uint32_t kAccProtected =    0x0004;  // field, method, ic
                                                      // ......
static constexpr uint32_t kAccJavaFlagsMask = 0xffff; // bits set from Java sources (low 16)

/**
 * access_flags_ 在 ArtField 中的偏移量满足：android-5.0 ~ android-11.0
 */
class ArtHookField final {
public:
  inline static void addAccessFlags(int access_flag, void* artField) {
    *reinterpret_cast<uint32_t*>((reinterpret_cast<uint32_t*>(artField) + 1)) |= access_flag;
  }

private:
  uint32_t reference_;
  uint32_t access_flags_ = 0;
  // ...... 不关注
};

} // namespace iwatch

#endif //IWATCH_ARTHOOKFIELD_H

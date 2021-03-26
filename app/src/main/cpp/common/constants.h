//
// Created by 葛祥林 on 1/14/21.
//

#pragma once

#ifndef IWATCH_CONSTANTS_H
#define IWATCH_CONSTANTS_H

namespace iwatch {

#define I_OK          (0)         /* no error */
#define I_ERR         (-1)        /* generic error */

extern int sdkVersion;
static constexpr int SDK_INT_ANDROID_5_0 = 21;
static constexpr int SDK_INT_ANDROID_5_1 = 22;
static constexpr int SDK_INT_ANDROID_6_0 = 23;
static constexpr int SDK_INT_ANDROID_7_0 = 24;
static constexpr int SDK_INT_ANDROID_10 = 29;
static constexpr int SDK_INT_ANDROID_11 = 30;

static constexpr uint32_t kAccPublic =       0x0001;  // class, field, method, ic
static constexpr uint32_t kAccPrivate =      0x0002;  // field, method, ic
static constexpr uint32_t kAccProtected =    0x0004;  // field, method, ic
static constexpr uint32_t kAccFinal =        0x0010;  // class, field, method, ic
static constexpr uint32_t kAccSynthetic =    0x1000;  // class, field, method, ic

// ......
static constexpr uint32_t kAccJavaFlagsMask = 0xffff; // bits set from Java sources (low 16)

#define IS_64BIT_OS (sizeof(void*) == 8)

static constexpr bool kEnableIndexIds = true;

} // namespace iwatch

#endif //IWATCH_CONSTANTS_H

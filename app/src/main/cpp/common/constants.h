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

static constexpr uint32_t kAccPublic =       0x0001;  // class, field, method, ic
static constexpr uint32_t kAccPrivate =      0x0002;  // field, method, ic
static constexpr uint32_t kAccProtected =    0x0004;  // field, method, ic
// ......
static constexpr uint32_t kAccJavaFlagsMask = 0xffff; // bits set from Java sources (low 16)

} // namespace iwatch

#endif //IWATCH_CONSTANTS_H

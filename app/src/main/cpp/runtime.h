//
// Created by 葛祥林 on 2021/3/7.
//

#ifndef IWATCH_RUNTIME_H
#define IWATCH_RUNTIME_H

#include <jni.h>
#include <memory>

#include "common/elf_op.h"
#include "common/log.h"

namespace iwatch {

// 位于: /art/runtime/runtime.h 中instance_ 表示当前运行时对象地址
// nm -g libart.so | grep 'instance_'
// 00000000006aa610 B _ZN3art12ProfileSaver9instance_E
// C++的符号规则(GNU C++)
static const char* RUNTIME_INSTANCE_SYM = "_ZN3art7Runtime9instance_E"; // static Runtime* instance_

class Runtime final {
public:
  Runtime() : instance_(nullptr) {}

  ~Runtime() {
    instance_ = nullptr;
  }

  void init(JNIEnv*, const std::shared_ptr<Elf>&& elf_op);

  /**
   * 表示当前运行时Runtime对象地址，相当于
   */
  void* getRuntime();

private:
  // 代表art虚拟机中当前Runtime对象指针 /art/runtime/runtime.h 中的 Current() 函数:
  // static Runtime* instance_;
  void* instance_;
};

} // namespace iwatch

#endif //IWATCH_RUNTIME_H

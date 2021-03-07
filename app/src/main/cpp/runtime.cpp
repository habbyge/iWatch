//
// Created by 葛祥林 on 2021/3/7.
//

#include "runtime.h"

namespace iwatch {

void Runtime::init(JNIEnv*, const std::shared_ptr<Elf>&& elf_op) {
  instance_ = reinterpret_cast<void*>(elf_op->dlsym_elf(RUNTIME_INSTANCE_SYM));
}

/**
 * @return 代表art虚拟机中当前Runtime对象指针
 */
void* Runtime::getRuntime() {
  return instance_;
}

}

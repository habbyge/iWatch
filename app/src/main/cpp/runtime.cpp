//
// Created by 葛祥林 on 2021/3/7.
//

#include "runtime.h"

namespace iwatch {

void* Runtime::self_ = nullptr;

void Runtime::init(JNIEnv*, const std::shared_ptr<Elf>& elf_op) {
  void** pInstance_ = reinterpret_cast<void**>(elf_op->dlsym_elf(RUNTIME_INSTANCE_SYM));
  instance_ = *pInstance_;

  auto currentFromGdb = reinterpret_cast<CurrentFromGdb>(elf_op->dlsym_elf(CurrentFromGdb_Syn));
  self_ = currentFromGdb();
  logd("Runtime::init, self_ = %p", self_);

  if (sdkVersion > SDK_INT_ANDROID_6_0) {
    useJitCompilation = reinterpret_cast<UseJitCompilation>(elf_op->dlsym_elf(UseJitCompilation_Syn));
  } else if (sdkVersion == SDK_INT_ANDROID_6_0) {
    useJitCompilation = reinterpret_cast<UseJitCompilation>(elf_op->dlsym_elf(UseJit_Syn));
  } else {
    useJitCompilation = nullptr; // android-5.x 未采用jit是false
  }
}

/**
 * @return 代表art虚拟机中当前Runtime对象指针
 */
void* Runtime::getRuntime() const noexcept {
  return instance_;
}

void* Runtime::currentThread() noexcept {
  logw("Runtime::init, currentThread=%p", self_);
  return self_;
}

bool Runtime::useJit() const noexcept {
  return useJitCompilation != nullptr && useJitCompilation();
}

}

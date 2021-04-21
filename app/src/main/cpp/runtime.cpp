//
// Created by 葛祥林 on 2021/3/7.
//

#include "runtime.h"

namespace iwatch {

void* Runtime::self_ = nullptr;

void Runtime::init(JNIEnv*, const std::shared_ptr<Elf>& elf_op, int sdkVersionCode) {
  void** pInstance_ = reinterpret_cast<void**>(elf_op->dlsym_elf(RUNTIME_INSTANCE_SYM));
  instance_ = *pInstance_;
  logd("Runtime::init, instance_ = %p", instance_);
  if (instance_ == nullptr) {
    return;
  }

  auto currentFromGdb = reinterpret_cast<CurrentFromGdb>(elf_op->dlsym_elf(CurrentFromGdb_SYM));
  self_ = currentFromGdb();
  logd("Runtime::init, self_ = %p, sdkVersionCode=%d", self_, sdkVersionCode);

  auto isVerificationEnabled = reinterpret_cast<IsVerificationEnabled>(elf_op->dlsym_elf(IsVerificationEnabled_SYM));
  bool verify_ = isVerificationEnabled(instance_);
  logd("Runtime::init, isVerificationEnabled = %d", verify_);
  if (verify_) { // 如果是class校验是打开的，则关闭
    if (sdkVersionCode >= SDK_INT_ANDROID_9) {
      auto disableVerifier = reinterpret_cast<DisableVerifier>(elf_op->dlsym_elf(DisableVerifier_SYM));
      disableVerifier(instance_);
      // 再次验证是否关闭
      verify_ = isVerificationEnabled(instance_);
      logi("Runtime::init, >= android-9, check again, isVerificationEnabled = %d", verify_);
    } else {
      // TODO: < 9.0，应该是关闭的
      logi("Runtime::init, < android-9, check again, isVerificationEnabled = %d", verify_);
    }
  }

  /*if (sdkVersionCode > SDK_INT_ANDROID_6_0) {
    useJitCompilation = reinterpret_cast<UseJitCompilation>(elf_op->dlsym_elf(UseJitCompilation_Syn));
  } else if (sdkVersionCode == SDK_INT_ANDROID_6_0) {
    useJitCompilation = reinterpret_cast<UseJitCompilation>(elf_op->dlsym_elf(UseJit_Syn));
  } else {
    useJitCompilation = nullptr; // android-5.x 未采用jit是false
  }*/
}

/**
 * @return 代表art虚拟机中当前Runtime对象指针
 */
void* Runtime::getRuntime() const noexcept {
  return instance_;
}

void* Runtime::currentThread() noexcept {
  logi("Runtime::init, currentThread=%p", self_);
  return self_;
}

/*bool Runtime::useJit() const noexcept {
  return useJitCompilation != nullptr && useJitCompilation();
}*/

} // namespace iWatch

//
// Created by 葛祥林 on 1/4/21.
//

#pragma once

#ifndef IWATCH_JNIENVEXT_H
#define IWATCH_JNIENVEXT_H

#include <jni.h>
#include <stdint.h>
#include "../runtime.h"

namespace art {

namespace mirror {

class JNIEnvExt : public JNIEnv {
public:
  void* GetSelf() {
    init();
    return self_;
  }

  void* GetVm() const {
    return vm_;
  }

private:
  // Link to Thread::Current().
  void* self_;
  // The invocation interface JavaVM.
  void* const vm_;

  inline void init() noexcept {
    if (self_ == nullptr) {
      self_ = iwatch::Runtime::currentThread();
    }
  }
};

} // mirror

} // art

#endif //IWATCH_JNIENVEXT_H

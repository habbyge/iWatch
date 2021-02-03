//
// Created by 葛祥林 on 1/4/21.
//

#pragma once

#ifndef IWATCH_JNIENVEXT_H
#define IWATCH_JNIENVEXT_H

#include <jni.h>
#include <stdint.h>

namespace art {

namespace mirror {

class JNIEnvExt : public JNIEnv {
public:
  void* GetSelf() const {
    return self_;
  }

  void* GetVm() const {
    return vm_;
  }

private:
  // Link to Thread::Current().
  void* const self_;
  // The invocation interface JavaVM.
  void* const vm_;
};

} // mirror

} // art

#endif //IWATCH_JNIENVEXT_H

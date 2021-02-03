//
// Created by 葛祥林 on 1/5/21.
//

#pragma once

#ifndef IWATCH_SCOPEDFASTNATIVEOBJECTACCESS_H
#define IWATCH_SCOPEDFASTNATIVEOBJECTACCESS_H

#include "JNIEnvExt.h"
#include "scoped_thread_state_change.h"

namespace art {

// Variant of ScopedObjectAccess that does no runnable transitions. Should only be used by "fast"
// JNI methods.
class ScopedFastNativeObjectAccess : public ScopedObjectAccessAlreadyRunnable {
public:
  explicit ScopedFastNativeObjectAccess(JNIEnv* env) : ScopedObjectAccessAlreadyRunnable(env) {}
  ~ScopedFastNativeObjectAccess() {}

private:
//  DISALLOW_COPY_AND_ASSIGN(ScopedFastNativeObjectAccess);
};

} // art

#endif //IWATCH_SCOPEDFASTNATIVEOBJECTACCESS_H

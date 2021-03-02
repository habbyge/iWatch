//
// Created by 葛祥林 on 1/5/21.
//

#pragma once

#ifndef IWATCH_SCOPED_THREAD_STATE_CHANGE_H
#define IWATCH_SCOPED_THREAD_STATE_CHANGE_H

#include "value_object.h"
#include "JNIEnvExt.h"

namespace art {

// Quickly access the current thread from a JNIEnv.
static inline void* ThreadForEnv(JNIEnv* env) {
  mirror::JNIEnvExt* full_env(static_cast<mirror::JNIEnvExt*>(env)); // copy-constructor
  return full_env->GetSelf();
}

// Assumes we are already runnable.
class ScopedObjectAccessAlreadyRunnable : public ValueObject {
//public:
//  Thread* Self() const {
//    return self_;
//  }
//
//  JNIEnvExt* Env() const {
//    return env_;
//  }
//
//  JavaVMExt* Vm() const {
//    return vm_;
//  }
//
//  bool ForceCopy() const;
//
//  /*
//   * Add a local reference for an object to the indirect reference table associated with the
//   * current stack frame.  When the native function returns, the reference will be discarded.
//   *
//   * We need to allow the same reference to be added multiple times, and cope with nullptr.
//   *
//   * This will be called on otherwise unreferenced objects. We cannot do GC allocations here, and
//   * it's best if we don't grab a mutex.
//   */
//  template<typename T>
//  T AddLocalReference(ObjPtr<mirror::Object> obj) const
//  REQUIRES_SHARED(Locks::mutator_lock_);
//
//  template<typename T>
//  ObjPtr<T> Decode(jobject obj) const REQUIRES_SHARED(Locks::mutator_lock_);
//
//  ALWAYS_INLINE bool IsRunnable() const;

protected:
//  ALWAYS_INLINE explicit ScopedObjectAccessAlreadyRunnable(JNIEnv* env)
//  REQUIRES(!Locks::thread_suspend_count_lock_);

//  ALWAYS_INLINE explicit ScopedObjectAccessAlreadyRunnable(Thread* self)
//  REQUIRES(!Locks::thread_suspend_count_lock_);

  // Used when we want a scoped JNI thread state but have no thread/JNIEnv. Consequently doesn't
  // change into Runnable or acquire a share on the mutator_lock_.
  // Note: The reinterpret_cast is backed by a static_assert in the cc file. Avoid a down_cast,
  //       as it prevents forward declaration of JavaVMExt.
//  explicit ScopedObjectAccessAlreadyRunnable(JavaVM* vm)
//      : self_(nullptr), env_(nullptr), vm_(reinterpret_cast<JavaVMExt*>(vm)) {}
  explicit ScopedObjectAccessAlreadyRunnable(JavaVM* vm) : self_(nullptr),
                                                           env_(nullptr),
                                                           vm_(reinterpret_cast<void*>(vm)) {}

  explicit ScopedObjectAccessAlreadyRunnable(JNIEnv* env) : self_(ThreadForEnv(env)),
                                                            env_(static_cast<mirror::JNIEnvExt*>(env)),
                                                            vm_(static_cast<mirror::JNIEnvExt*>(env)->GetVm()) {}

  // Here purely to force inlining.
  //  ALWAYS_INLINE ~ScopedObjectAccessAlreadyRunnable() {}
  ~ScopedObjectAccessAlreadyRunnable() {}
//
//  static void DCheckObjIsNotClearedJniWeakGlobal(ObjPtr<mirror::Object> obj)
//  REQUIRES_SHARED(Locks::mutator_lock_);

  // Self thread, can be null.
  //  Thread* const self_;
  void* const self_; // 当前线程
  // The full JNIEnv.
  //  JNIEnvExt* const env_;
  void* const env_; // 当前线程的JNI环境
  // The full JavaVM.
  //  JavaVMExt* const vm_;
  void* const vm_; // 当前进程的虚拟机
};

// Entry/exit processing for transitions from Native to Runnable (ie within JNI functions).
//
// This class performs the necessary thread state switching to and from Runnable and lets us
// amortize the cost of working out the current thread. Additionally it lets us check (and repair)
// apps that are using a JNIEnv on the wrong thread. The class also decodes and encodes Objects
// into jobjects via methods of this class. Performing this here enforces the Runnable thread state
// for use of Object, thereby inhibiting the Object being modified by GC whilst native or VM code
// is also manipulating the Object.
//
// The destructor transitions back to the previous thread state, typically Native. In this state
// GC and thread suspension may occur.
//
// For annotalysis the subclass ScopedObjectAccess (below) makes it explicit that a shared of
// the mutator_lock_ will be acquired on construction.
class ScopedObjectAccessUnchecked : public ScopedObjectAccessAlreadyRunnable {
public:
  explicit ScopedObjectAccessUnchecked(JNIEnv* env) : ScopedObjectAccessAlreadyRunnable(env) {}

  ~ScopedObjectAccessUnchecked() {}
};

// Annotalysis helping variant of the above.
class ScopedObjectAccess : public ScopedObjectAccessUnchecked {
public:
  explicit ScopedObjectAccess(JNIEnv* env) : ScopedObjectAccessUnchecked(env) {}

  // Base class will release share of lock. Invoked after this destructor.
  ~ScopedObjectAccess() {
  }
};

} // art

#endif //IWATCH_SCOPED_THREAD_STATE_CHANGE_H

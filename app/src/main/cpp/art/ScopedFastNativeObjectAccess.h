//
// Created by 葛祥林 on 1/5/21.
//

#ifndef IWATCH_SCOPEDFASTNATIVEOBJECTACCESS_H
#define IWATCH_SCOPEDFASTNATIVEOBJECTACCESS_H

#include "JNIEnvExt.h"

namespace art {

class ValueObject {
  private:
//  DISALLOW_ALLOCATION();
};

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

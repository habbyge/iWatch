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

// 监听 runtime 退出函数，可以hook
// void CallExitHook(jint status);
//static const char* CallExitHook_Sym = "_ZN3art7Runtime12CallExitHookEi";

// 这里是 art/runtime/runtime.h void DumpForSigQuit(std::ostream& os)
// 可以看到这里其实是把Art中的运行时系统的Runtime对象中在信号SigQuit发生时，runtime退出前，保存Runtime中的数据到
// os中，这里一般传入的是cerr对象，用于发生crash时，抓取运行时系统(runtime)的上下文
//static const char* DumpForSigQuit_Sym =
//    "_ZN3art7Runtime14DumpForSigQuitERNSt3__113basic_ostreamIcNS1_11char_traitsIcEEEE";

// art/runtime/thread.h 中:
using CurrentFromGdb = void* (*)(); // Thread* CurrentFromGdb()
static const char* CurrentFromGdb_Syn = "_ZN3art6Thread14CurrentFromGdbEv";
 // CurrentFromGdb -> _ZN3art6Thread14CurrentFromGdbEv 是否只能在gdb debug 时才能获取？不是，release一样可以获取
// GetCurrentMethod() -> _ZNK3art6Thread16GetCurrentMethodEPjbb
// DumpJavaStack -> _ZNK3art6Thread13DumpJavaStackERNSt3__113basic_ostreamIcNS1_11char_traitsIcEEEEbb

//
// SuspendVM -> _ZN3art3Dbg9SuspendVMEv
// ResumeVM -> _ZN3art3Dbg8ResumeVMEv

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

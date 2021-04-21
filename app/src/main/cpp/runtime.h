//
// Created by 葛祥林 on 2021/3/7.
//

#ifndef IWATCH_RUNTIME_H
#define IWATCH_RUNTIME_H

#include <jni.h>
#include <memory>

#include "common/elf_op.h"
#include "common/log.h"
#include "common/constants.h"

namespace iwatch {

// android-10~11，符号名称未变，(除了新增的)可以看出符号比较稳定，起始这些符号看似晦涩难懂，实际上是有规则的，这就是
// C++的符号规则，这从另外一方面也证明了该方案的稳定性，是可行的。

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
static const char* CurrentFromGdb_SYM = "_ZN3art6Thread14CurrentFromGdbEv";
// CurrentFromGdb -> _ZN3art6Thread14CurrentFromGdbEv 是否只能在gdb debug 时才能获取？不是，release一样可以获取
// GetCurrentMethod() -> _ZNK3art6Thread16GetCurrentMethodEPjbb
// DumpJavaStack -> _ZNK3art6Thread13DumpJavaStackERNSt3__113basic_ostreamIcNS1_11char_traitsIcEEEEbb
// DumpStack -> _ZNK3art6Thread9DumpStackERNSt3__113basic_ostreamIcNS1_11char_traitsIcEEEEbP12BacktraceMapb
// static bool IsAotCompiler() -> _ZN3art6Thread13IsAotCompilerEv

// 新版本(Android-11)已经废弃:
// SuspendVM -> _ZN3art3Dbg9SuspendVMEv
// ResumeVM -> _ZN3art3Dbg8ResumeVMEv

// art/runtime/oat_file_manager.h 中 OpenDexFilesFromOat/OpenDexFilesFromOat/OpenDexFilesFromOat_Impl
// _ZN3art14OatFileManager19OpenDexFilesFromOatEONSt3__16vectorINS_6MemMapENS1_9allocatorIS3_EEEEP8_jobjectP13_jobjectArrayPPKNS_7OatFileEPNS2_INS1_12basic_stringIcNS1_11char_traitsIcEENS4_IcEEEENS4_ISK_EEEE
// _ZN3art14OatFileManager19OpenDexFilesFromOatEPKcP8_jobjectP13_jobjectArrayPPKNS_7OatFileEPNSt3__16vectorINSB_12basic_stringIcNSB_11char_traitsIcEENSB_9allocatorIcEEEENSG_ISI_EEEE
// _ZN3art14OatFileManager24OpenDexFilesFromOat_ImplEONSt3__16vectorINS_6MemMapENS1_9allocatorIS3_EEEEP8_jobjectP13_jobjectArrayPPKNS_7OatFileEPNS2_INS1_12basic_stringIcNS1_11char_traitsIcEENS4_IcEEEENS4_ISK_EEEE

// android 5.0/5.1/6.0(interpreter + aot)
// android 7.0/7.1及其以后的版本，ART移入了全新的Hybrid模式(interpreter + jit + aot)
// bool UseJitCompilation() const;
//static const char* UseJitCompilation_Syn = "_ZNK3art7Runtime17UseJitCompilationEv"; // >= android-7.0
//using UseJitCompilation = bool (*)();
// bool UseJit() const;
//static const char* UseJit_Syn = "_ZNK3art7Runtime6UseJitEv"; // android-6.x
// android-5.0.5.1 没有这个选项，阅读了源码，5.x是直接采用AOT进程安装编译的

// FIXME: 如果要用jit load或compile功能
// ------------------ android-11 ------------------
// JIT compiler
// static void* jit_library_handle_;
// static JitCompilerInterface* jit_compiler_;
// static JitCompilerInterface* (*jit_load_)(void);
// template <typename T> static bool LoadSymbol(T*, const char* symbol, std::string* error_msg);
// 在libart.so中查找：
// flame:/apex/com.android.art/lib64 $ readelf -s libart.so | grep 'jit_compile'
//  2394: 00000000006ab610     8 OBJECT  GLOBAL PROTECTED 23 _ZN3art3jit3Jit13jit_compiler_E
// 23991: 00000000006ab610     8 OBJECT  GLOBAL PROTECTED 23 _ZN3art3jit3Jit13jit_compiler_E
// readelf -s libart.so | grep 'jit_load'
//  2163: 00000000006ab618     8 OBJECT  GLOBAL PROTECTED 23 _ZN3art3jit3Jit9jit_load_E
// 24101: 00000000006ab618     8 OBJECT  GLOBAL PROTECTED 23 _ZN3art3jit3Jit9jit_load_E

// ------------------ android-10 ------------------
// 位于：art/runtime/jit/jit.h
// 在 art/runtime/jit/jit.cc 中实现了 jit的代码，例如: bool Jit::LoadCompilerLibrary(std::string* error_msg);
// JIT compiler
// static void* jit_library_handle_;
// static void* jit_compiler_handle_;
// static void* (*jit_load_)(void);
// static void (*jit_unload_)(void*);
// static bool (*jit_compile_method_)(void*, ArtMethod*, Thread*, bool, bool);
// static void (*jit_types_loaded_)(void*, mirror::Class**, size_t count);
// static void (*jit_update_options_)(void*);
// static bool (*jit_generate_debug_info_)(void*);
// template <typename T> static bool LoadSymbol(T*, const char* symbol, std::string* error_msg);
// nm -g libart.so|grep 'jit_load'
// 00000000005cf978 B _ZN3art3jit3Jit9jit_load_E
// nm -g libart.so|grep 'jit_compile'
// 00000000005cf988 B _ZN3art3jit3Jit19jit_compile_method_E
// 00000000005cf970 B _ZN3art3jit3Jit20jit_compiler_handle_E

// nm -g libart64.so|grep 'jit_'
// 00000000006ab610 B _ZN3art3jit3Jit13jit_compiler_E
// 00000000006ab608 B _ZN3art3jit3Jit19jit_library_handle_E
// 00000000006ab618 B _ZN3art3jit3Jit9jit_load_E
// 00000000006a9e90 B _ZN3art5Locks9jit_lock_E
// 00000000006ac430 B _ZN3art6Thread21jit_sensitive_thread_E
// 00000000006a7548 D __jit_debug_descriptor
// 0000000000334f7c T __jit_debug_register_code
// 00000000006a7540 D __jit_debug_register_code_ptr
// 00000000006a7538 D g_art_sizeof_jit_code_entry
// 00000000006a753c D g_art_sizeof_jit_descriptor

// 去掉Dex的Verify功能，避免检查class、method，导致的宿主中的类和patch中类_CF，在寄存器(eg: v2)中不一致，导致检验失败：
// 发生 java.lang.VerifyError，目前已经用了生成patch(apkpatchplus工程)时，去掉_CF后缀的方式解决，但这不是一个优雅的
// 方案，更优雅或hack的方法是这里：针对补丁的_CF类及其对应的原始类，直接禁止掉检查。
// art/runtime/runtime.cc
// void Runtime::DisableVerifier();
// 对应art中的符号是：_ZN3art7Runtime15DisableVerifierEv 适配性：支持：android-9/10/11，5/6/7/8方案是: verify_
// 默认是false 或 verifier::VerifyMode::kNone，所以没问题。
using DisableVerifier = void (*)(void* runtime); // 参数是Runtime*
static const char* DisableVerifier_SYM = "_ZN3art7Runtime15DisableVerifierEv";
// bool Runtime::IsVerificationEnabled() const; 支持 >= andoid-5.0
using IsVerificationEnabled = bool (*)(void* runtime); // 参数是Runtime*
static const char* IsVerificationEnabled_SYM = "_ZNK3art7Runtime21IsVerificationEnabledEv";

class Runtime final {
public:
  Runtime() : instance_(nullptr) {}

  ~Runtime() {
    instance_ = nullptr;
    self_     = nullptr;
  }

  void init(JNIEnv*, const std::shared_ptr<Elf>& elf_op, int sdkVersionCode);

  /**
   * 表示当前运行时Runtime对象地址，相当于
   */
  void* getRuntime() const noexcept;
  static void* currentThread() noexcept;

  /*bool useJit() const noexcept;*/

private:
  // 代表art虚拟机中当前Runtime对象指针 /art/runtime/runtime.h 中的 Current() 函数:
  // static Runtime* instance_; Runtime::instance_字段，是一个全局变量，使用readelf -s libart.so可以查看
  void* instance_;
  static void* self_; // 当前 Thread*

  /*UseJitCompilation useJitCompilation;*/
};

} // namespace iWatch

#endif // IWATCH_RUNTIME_H

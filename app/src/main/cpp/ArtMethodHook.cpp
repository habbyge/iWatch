//
// Created by habbyge on 1/26/21.
//

#include "ArtMethodHook.h"

//
// Created by 葛祥林 on 1/8/21.
//

#include "iwatch_impl.h"
#include "ArtRestore.h"
#include "common/constants.h"
#include "art/art_method5_0.h"

//#include <art/runtime/jni/jni_internal.h>
//#include <exception> C/C++ 的 Exception

// -----------------------------------------------------------------------------------------------
// 查看 art 虚拟机代码技巧：
// 1. Java中的class全名(例如：java/lang/reflect/Method)，对应 Art 中的源文件是：
// art/runtime/native/java_lang_reflect_Method.h
// art/runtime/native/java_lang_reflect_Method.cc
// 即标准的 JNI 接口协议.
// -----------------------------------------------------------------------------------------------

// 方案限制：inline 函数 fix 会失败.
// 这里需要考虑，对 inline 函数的影响，内联函数是否有 Method 对象，是否能够被成功替换 ？
// 需要研究：直接 ArtMethod 整体替换的方案，是否能够解决 inline 问题 ？
// 这篇文章对 inline 的影响有详尽研究(同时也收录到了我的有道云笔记中了)：
// https://cloud.tencent.com/developer/article/1005604x
// 内联的源代码位置: art/compiler/optimizing/inliner.cc 中的 HInliner::Run() 函数中.
// 实际上，针对 "数据统计补丁" 来说，一般很少有机会在 inline 方法中插桩，即使需要，也可以通过其他手段突破.
// 因此，作为 局限性之一的 inline fix 失败，实际上对 数据统计补丁来说，影响并不大，凡是并不仅能尽善尽美，接收缺陷.
// 我研究了一阵子，就 replace inline 函数来说，从 native 层来说，几乎无解。。。
// 如果在 Java 层给每一个函数插桩一个 try-catch，来阻止编译期的 inline，实际上是非常糟糕的做法。。不能被接受
// 实际上所有的方案，都不能尽善尽美，做不到 inline 的话，统计上报需求来了后，我们自己评估下，如果需要在可能被
// inline 的方法中插桩的话，我们就通过发版本方式来统计。。其实也还好.
// 我 review 了下，我之前的那么多统计上报代码，在可能被 inline 的地方添加上报的可能性几乎么有。

// Android-11 适配问题 ------> fix

// jni.h 中的实现，在 Art 虚拟机中的目录是 art/runtime/jni/ 目录下，例如：art/runtime/jni/jni_internal.cc

// dex diff

// iwatch 思考的问题点 - done
// 1. 需要加锁 + 条件变量来保护？？？？？？难做到......
// 1.1 hook替换时，原始函数刚好执行到这里；需要互斥
// 1.2 正在恢复时，业务函数正好执行到这里，反之情况也一样；
// 答案：
// 方案1: 有个办法是给这块儿内存(原始或替换后的方法地址)加上锁，谁访问都要互斥；不好，影响性能，而且没必要.
// 方案2: 如果啥都不修改，是否表示当前函数地址在某一时刻的调用，是原子的？？？？？？或者更精确一点是：获取原始函数地址的操作是原子的.
//       经过与fun哥讨论，基于一个原理是：汇编的一条指令是原子的，超过一条指令就不是原子的，函数call指令，在取函数地址时，从内存取
//       到寄存器时，是一条汇编指令，也就是原子的，那么我这里hook替换时，是把新的函数地址从寄存器替换到存储原始函数的地址的内存中，
//       也是一条指令，都具有原子性，因此不存在互斥竞态问题。done!!!!!!

/**
 * 这里仅仅只有一个目的，就是为了计算出不同平台下，每个 art::mirror::ArtMethod 大小，
 * 这里 jmethodID 就是 ArtMethod.
 * 比起 ArtFix，iWatch 方案屏蔽细节、尽量通用，没有适配性。
 */

namespace iwatch {

void ArtMethodHook::initArtMethodLessEqual10(JNIEnv* env) {
  jclass ArtMethodSizeClass = env->FindClass(computeArtMethodSize_ClassName);
  auto artMethod1 = env->GetStaticMethodID(ArtMethodSizeClass, "func1", "()V");
  auto artMethod2 = env->GetStaticMethodID(ArtMethodSizeClass, "func2", "()V");
  env->DeleteLocalRef(ArtMethodSizeClass); // 速度释放局部引用，避免阻碍Java层gc该对象
  artMethodSizeV1 = reinterpret_cast<uintptr_t>(artMethod2) - reinterpret_cast<uintptr_t>(artMethod1);

  // artMethodSize = sizeof(ArtMethod);
  logi("artMethodSize-1 = %zu, %p, %p", artMethodSizeV1,
                                        reinterpret_cast<void*>(reinterpret_cast<uintptr_t>(artMethod2)),
                                        reinterpret_cast<void*>(reinterpret_cast<uintptr_t>(artMethod1)));
}

void ArtMethodHook::initArtMethod1(JNIEnv* env, const std::shared_ptr<Elf>& elf_op, jobject method1, jobject method2) {
  try {
//   FromReflectedMethod = reinterpret_cast<FromReflectedMethod_t>(
//       dlsym_elf(elf_op->getLoadAddr(), FromReflectedMethod_Sym));

    FromReflectedMethod = reinterpret_cast<FromReflectedMethod_t>(elf_op->dlsym_elf(FromReflectedMethod_Sym));
    logi("initArtMethod1, FromReflectedMethod=%p", FromReflectedMethod);
    const void* artMethod1 = getArtMethod(env, method1);
    logi("initArtMethod1, method1=%p, %p", artMethod1, method1);
    const void* artMethod2 = getArtMethod(env, method2);
    // jmethodID, jmethodID, ArtMethod*, ArtMethod*
    logi("initArtMethod1, method2=%p, %p", artMethod2, method2);

    // 这里动态获取 ArtMethod 大小的原理是：在 Art 虚拟机中的 Class 类中的 methods_ 字段决定的：
    // ArtMethod按照类中方法声明顺序依次紧密的排列在 methods_ 字段表示的内存中.
    artMethodSizeV1 = reinterpret_cast<uintptr_t>(artMethod2) - reinterpret_cast<uintptr_t>(artMethod1);
    logi("initArtMethod1, artMethodSizeV1 = %zu", artMethodSizeV1);
  } catch (std::exception& e) {
    loge("initArtMethod1, eception: %s", e.what());
    clear_exception(env);
    artMethodSizeV1 = -1;
  }
}

void ArtMethodHook::initArtMethod2(JNIEnv* env, const std::shared_ptr<Elf>& elf_op, jobject method1, jobject method2) {
  FindMethodJNI = reinterpret_cast<FindMethodJNI_t>(elf_op->dlsym_elf(FindMethodJNI_Sym));
  logi("initArtMethod2, FindMethodJNI=%p", FindMethodJNI);

  jclass ArtMethodSizeClass = env->FindClass(computeArtMethodSize_ClassName);

  void* artMethod11 = nullptr;
  void* artMethod12 = nullptr;
  try {
    jmethodID jmethod1 = env->FromReflectedMethod(method1);
    if (isIndexId<kEnableIndexIds>(jmethod1)) {
      artMethod11 = getArtMethod(env, ArtMethodSizeClass, "func1", "()V", true);
      logi("initArtMethod2, artMethod11=%p", artMethod11);
    } else {
      artMethod11 = reinterpret_cast<void*>(jmethod1);
    }

    jmethodID jmethod2 = env->FromReflectedMethod(method2);
    if (isIndexId<kEnableIndexIds>(jmethod2)) {
      artMethod12 = getArtMethod(env, ArtMethodSizeClass, "func2", "()V", true);
      // jmethodID, jmethodID, ArtMethod*, ArtMethod*
      logi("initArtMethod2, artMethod22=%p", artMethod12);
    } else {
      artMethod12 = reinterpret_cast<void*>(jmethod2);
    }

    env->DeleteLocalRef(ArtMethodSizeClass);

    // 这里动态获取 ArtMethod 大小的原理是：在 Art 虚拟机中的 Class 类中的 methods_ 字段决定的
    artMethodSizeV2 = reinterpret_cast<uintptr_t>(artMethod12) - reinterpret_cast<uintptr_t>(artMethod11);
    logi("initArtMethod2, artMethodSizeV2 = %zu", artMethodSizeV2);
  } catch (std::exception& e) {
    loge("initArtMethod2, eception: %s", e.what());
    clear_exception(env);
    artMethodSizeV2 = -1;
  }
}

void* ArtMethodHook::getArtMethodLessEqual10(JNIEnv* env, jobject method) const {
  return reinterpret_cast<void*>(env->FromReflectedMethod(method));
}

/**
 * 方案1
 */
void* ArtMethodHook::getArtMethod(JNIEnv* env, jobject method) {
  try {
    const art::ScopedFastNativeObjectAccess soa{env};
    return FromReflectedMethod(soa, method);
  } catch (std::exception& e) {
    loge("getArtMethod1, eception: %s", e.what());
    clear_exception(env);
    return nullptr;
  }
}

/**
 * 方案2
 */
void* ArtMethodHook::getArtMethod(JNIEnv* env, jclass java_class, const char* name, const char* sig, bool is_static) {
  try {
    art::ScopedObjectAccess soa(env);
    return FindMethodJNI(soa, java_class, name, sig, is_static);
  } catch (std::exception& e) {
    loge("getArtMethod2, eception: %s", e.what());
    clear_exception(env);
    return nullptr;
  }
}

/**
 * http://aosp.opersys.com/ 中查看各个版本的art_method.h 得到：
 *
 * 这个函数暂时不使用不能设置method的access flag，发生Crash：
 * 2021-04-07 13:15:33.855 7732-7732/? E/AndroidRuntime: FATAL EXCEPTION: main
 * Process: com.tencent.mm, PID: 7732
 * java.lang.IncompatibleClassChangeError: The method 'java.lang.String com.tencent.mm.plugin.finder.j.a.a.an(long, long)' was expected to be of type direct but instead was found to be of type virtual (declaration of 'com.tencent.mm.plugin.finder.j.a.a' appears in /data/app/~~StCyOrIiaxMVH-yp-cmUZw==/com.tencent.mm-ANrWQZ9Yy6xkFQtM-YjnKw==/base.apk!classes2.dex)
 * at com.tencent.mm.plugin.finder.j.a.a.a(SourceFile:246)
 * at com.tencent.mm.plugin.finder.j.a.a$a.a(SourceFile:51)
 * at com.tencent.mm.plugin.finder.j.a.d.a(SourceFile:503)
 * at com.tencent.mm.plugin.finder.j.a.d.a(SourceFile:416)
 * at com.tencent.mm.plugin.finder.j.a.d.b(SourceFile:250)
 * at com.tencent.mm.plugin.finder.j.a.d.a(SourceFile:60)
 * at com.tencent.mm.plugin.finder.j.a.a.a(SourceFile:152)
 * at com.tencent.mm.plugin.finder.j.a.k.a(SourceFile:1035)
 * at com.tencent.mm.plugin.finder.nearby.live.e$f.onScrolled(SourceFile:300)
 * 原因：查看 class_linker.cc中的ResolveMethod()可知，resolve->CheckIncompatibleClassChange()必须返回false，即
 * 兼容，从crash栈中能够看到是 kAccDirect 被搞成了 kAccVirtual 了，再看: kAccDirect = kAccStatic | kAccPrivate | kAccConstructor，
 * 那么很明显，这里 setAccessPublic() 不能调用了，如果调用成 public 函数，则会被认为是 kAccVirtual，不符。
 */
void ArtMethodHook::setAccessPublic(JNIEnv* env, jobject method) {
  void* artMethod;
  if (sdkVersion <= SDK_INT_ANDROID_10) {
    artMethod = getArtMethodLessEqual10(env, method);
  } else {
    artMethod = getArtMethod(env, method);
  }
  logi("ArtMethodHook, setAccessPublic, %p", artMethod);

  uint32_t* access_flags_addr{nullptr};
  uint32_t access_flags_{0u};

  uint32_t step = -1u;
  if (sdkVersion == SDK_INT_ANDROID_5_0) { // 5.0.x
    auto* artMethod5_0 = reinterpret_cast<art::mirror::ArtMethod5_0*>(artMethod);
    access_flags_ = artMethod5_0->access_flags_;
    if ((artMethod5_0->access_flags_ & kAccSynthetic) == kAccSynthetic) {
      // 由于内部类合成的字段(例如：外部类的对象字段)，不能设置其访问权限
//      artMethod5_0->access_flags_ &= ~kAccSynthetic; // clear Synthetic
//      artMethod5_0->access_flags_ |= (kAccPublic | kAccFinal);
      logd("synthetic setAccessPublic sdk5.0 !");
    } else {
      /*artMethod5_0->access_flags_ = (artMethod5_0->access_flags_ & (~kAccPrivate) & (~kAccProtected)) | kAccPublic;*/
    }
    access_flags_addr = &(artMethod5_0->access_flags_);
  } else if (sdkVersion >= SDK_INT_ANDROID_5_1 && sdkVersion <= SDK_INT_ANDROID_6_0) { // 5.1.x ~ 6.0.x
    step = 3u;
  } else if (sdkVersion >= SDK_INT_ANDROID_7_0/* && sdkVersion <= SDK_INT_ANDROID_11*/) { // 7.0.x ~ 11.0.x
    step = 1u;
  }

  if (step > 0) {
    access_flags_addr = reinterpret_cast<uint32_t*>(artMethod) + step;
    access_flags_ = *access_flags_addr;
    // What is the meaning of “static synthetic”? eg: .method static synthetic access$0()Lcom/package/Sample;
    // answer: Synthetic field, (2)
    // A compiler-created field that links a local inner class to a block's local variable or reference type parameter.
    // See also The JavaTM Virtual Machine Specification (§4.7.6) or Synthetic Class in Java.
    if ((access_flags_ & kAccSynthetic) == kAccSynthetic) {
      // need clear Synthetic 防止内部类生成这个合成函数时，多余的权限检查 ??????
      // java.lang.VerifyError:
//      *access_flags_addr &= ~kAccSynthetic;
//      *access_flags_addr |= (kAccPublic | kAccFinal);
      logd("synthetic setAccessPublic !");
      // 由于内部类合成的字段(例如：外部类的对象字段)，不能设置其访问权限
    } else if (((access_flags_ & kAccFinal) == kAccFinal) && ((access_flags_ & kAccPublic) == kAccPublic)) {
//      *access_flags_addr &= ~kAccFinal; // 清理掉final
      logd("method setAccessPublic: remove final !");
    } else {
//    *access_flags_addr = ((*access_flags_addr) & (~kAccPrivate) & (~kAccProtected)) | kAccPublic;
    }

    *access_flags_addr = ((*access_flags_addr) & (~kAccPrivate) & (~kAccProtected)) | kAccPublic;
  }

  logw("ArtMethodHook::setAccessPublic, sdkVersion=%d, access_flags_addr=%p, access_flags=%u -> %u",
       sdkVersion, access_flags_addr, access_flags_, *access_flags_addr);
}

} // namespace iwatch

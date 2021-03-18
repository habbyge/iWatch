//
// Created by 葛祥林 on 2021/3/5.
//

#ifndef IWATCH_INNER_CLASS_H
#define IWATCH_INNER_CLASS_H

namespace iwatch {

/**
 * todo Created by habbyge on 2020/11/24.
 *  内部列中函数的hook，难点在于：
 *  1. 如果[新增]了外部类，并调用了外部类的方法或字段，则会新增(kAccSynthetic，位于art/libdexfile/dex/modifiers.h)
 *     一个外部类的this指针在内部类中，且这个字段实际上是旧的外部类，而不是新的外部类的.，todo ing......
 *     这样就出坑了.
 */

// 查看：nm -g libart.so | grep 'GetInnerClassFlags'
// art/runtime/mirror/class.h
// 函数原型：int32_t Class::GetInnerClassFlags(Handle<Class> h_this, int32_t default_value)
// 获取一个class是否是内部类的标记，其中 andorid-10/android-11 都是这个符号
const char* GetInnerClassFlags_Sym = "_ZN3art6mirror5Class18GetInnerClassFlagsENS_6HandleIS1_EEi";

class InnerClass {
};

} // namespace iwatch

#endif //IWATCH_INNER_CLASS_H

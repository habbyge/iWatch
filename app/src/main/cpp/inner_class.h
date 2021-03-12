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
class InnerClass {
};

} // namespace iwatch

#endif //IWATCH_INNER_CLASS_H

package com.habbyge.iwatch;

import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Created by habbyge on 2020/11/25.
 *
 * 为何选择 Native 层方案，而不是 tinker、robust 方案？
 * 1. Tinker，发补丁流程复杂，且申请复杂(其他组控制补丁版本节奏)，版本号数量有限，主要用于解决业务bug fix；
 * 2. Q-Zone 等类似 nuwa 等基于 dexElements[dex] 原理的方案：同理，数据统计补丁要想不影响 tinker 补丁的发布，
 *    就不能影响到Java代码层本身，试想，如果数据统计补丁先生效了，就会把当前的Java代码修改了，那么 tinker 补丁在下
 *    发下来后，在合成阶段，就会出坑，因为 base 版本的代码不同了.
 * 3. Robust，基于 Instant Run 原理，使用 gradle plugin 向每个方法前插入一段逻辑代码，修复bug后的代码逻辑，通过
 *    DexClassLoader 加载到内存中后，并反射创建对象，赋值给目标方法中的逻辑对象，即可替换为补丁包中的新方法.
 *    按讲这个方案是对 DexClassloader 正常使用，兼容性也好，且也是下载后即可生效，那么我何为没有选择呐？
 *    我认为升级版的AndFix，即 iWatch 更简单、且没有负担，即 Robust 在编译时对每个方法都预插桩了一段逻辑代码，这样
 *    的做法不是很优雅和浪费，而且会造成内联失效，数据统计补丁的要求只是为了在某些方法中插入统计代码，并不需要在每一个
 *    方法中都插桩，因此 Robust 会造成大量浪费，这里显然不适合需求。同时 iWatch 也没有版本兼容性问题，即时生效，拥有
 *    Robust 一样的能力和稳定性，同时更加轻便，所以我认为更合适.
 * 所以，这里不能对原始代码做修改，那么显而易见，就只能通过改变 art 虚拟机中的指针，来指向统计补丁代码.
 */
public class MethodHook {
    private static final String TAG = "iWatch.MethodHook";

    // m1、m2两个方法必须是相邻的，用于计算 ArtMethod 大小
    public static void m1() {
    }
    public static void m2() {
    }
    // field1、field2 两个字段必须是相邻的，用于计算 ArtField 大小
    public static Object field1;
    public static Object field2;

    private final Method srcMethod;
    private final Method hookMethod;

    private long backupMethodPtr = 0L; // 原方法地址

    public MethodHook(Method src, Method dest) {
        srcMethod = src;
        srcMethod.setAccessible(true);

        hookMethod = dest;
        hookMethod.setAccessible(true);
    }

    public void hook() {
//        if (backupMethodPtr == 0L) {
            backupMethodPtr = hookMethod(srcMethod, hookMethod);
//        }
        Log.i(TAG, "hook success: " + backupMethodPtr);
    }

    public void restore() {
        if (backupMethodPtr != 0L) {
            Log.i(TAG, "restore  begin");
            restoreMethod(srcMethod, backupMethodPtr);
            Log.i(TAG, "restore  success");
            backupMethodPtr = 0L;
        }
    }

    public void callOrigin(Object receiver, Object... args) throws InvocationTargetException,
                                                                   IllegalAccessException {

        if (backupMethodPtr != 0L) {
            restore();
            srcMethod.invoke(receiver, args);
            hook();
        } else {
            srcMethod.invoke(receiver, args);
        }
    }

    public static void hookField2(Field src, Field dst) {
        hookField(src, dst);
    }

    public static void hookClass2(String clazzName) {
        long addr = hookClass(clazzName.replace(".", "/"));
        Log.i(TAG, "hookClass2, addr=" + addr);
    }

    private static native long hookMethod(Method src, Method dst);
    private static native Method restoreMethod(Method src, long methodPtr);
    private static native long hookField(Field src, Field dst);
    private static native long hookClass(String className);

    static {
        System.loadLibrary("iWatch");
    }
}

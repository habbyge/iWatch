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
 * 2. robust，同理，数据统计补丁要想不影响tinker补丁的发布，就不能影响到Java代码层本身，试想，如果数据统计
 *    补丁先生效了，就会把当前的Java代码修改了，那么tinker补丁在下发下来后，在合成阶段，就会出坑。base版本
 *    的代码不同了.
 * 所以，这里不能对原始代码做修改，那么显而易见，就只能通过改变art虚拟机中的指针指向来指向统计补丁代码了.
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
        if (backupMethodPtr == 0L) {
            backupMethodPtr = hookMethod(srcMethod, hookMethod);
        }
    }

    public void restore() {
        if (backupMethodPtr != 0L) {
            Log.i(TAG, "restore  begin");
            restoreMethod(srcMethod, backupMethodPtr);
            Log.i(TAG, "restore  success");
            backupMethodPtr = 0L;
        }
    }

    public void callOrigin(Object receiver, Object... args) throws
            InvocationTargetException, IllegalAccessException {

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

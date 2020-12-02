package com.habbyge.iwatch;

import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Created by habbyge on 2020/11/25.
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

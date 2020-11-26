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

    public static void m1() {
    }
    public static void m2() {
    }

    public static Object field1;
    public static Object field2;

    private final Method srcMethod;
    private final Method hookMethod;

    private long backupMethodPtr; // 原方法地址

    public MethodHook(Method src, Method dest) {
        srcMethod = src;
        srcMethod.setAccessible(true);

        hookMethod = dest;
        hookMethod.setAccessible(true);
    }

    public void hook() {
        if (backupMethodPtr == 0) {
            backupMethodPtr = hookMethod(srcMethod, hookMethod);
        }
    }

    public void restore() {
        if (backupMethodPtr != 0) {
            Log.i(TAG, "restore  begin");
            restoreMethod(srcMethod, backupMethodPtr);
            Log.i(TAG, "restore  success");
            backupMethodPtr = 0L;
        }
    }

    public void callOrigin(Object receiver, Object... args) throws
            InvocationTargetException, IllegalAccessException {

        if (backupMethodPtr != 0) {
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

    private static native long hookMethod(Method src, Method dst);
    private static native Method restoreMethod(Method src, long methodPtr);
    private static native long hookField(Field src, Field dst);

    static {
        System.loadLibrary("iWatch");
    }
}

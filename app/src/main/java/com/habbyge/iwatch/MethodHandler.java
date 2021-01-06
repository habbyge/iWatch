package com.habbyge.iwatch;

import android.util.Log;

import androidx.annotation.Keep;

import com.habbyge.iwatch.util.ReflectUtil;
import com.habbyge.iwatch.util.StringUtil;
import com.habbyge.iwatch.util.Type;

import java.lang.reflect.Method;

/**
 * Created by habbyge on 2021/1/5.
 */
@Keep
final class MethodHandler {
    private static final String TAG = "iWatch.MethodHandler";

    static void init() {
        MethodHook.init();
    }

    static void hook(String className1, String funcName1, Class<?>[] paramTypes1,
                     Class<?> returnType1, boolean isStatic1,
                     String className2, String funcName2, Class<?>[] paramTypes2,
                     Class<?> returnType2, boolean isStatic2) {

        if (StringUtil.isEmpty(className1) || StringUtil.isEmpty(funcName1)
                || StringUtil.isEmpty(className2) || StringUtil.isEmpty(funcName2)
                || returnType1 == null || returnType2 == null) {

            throw new NullPointerException("MethodHandler.hook, param is null");
        }

        Method method1 = ReflectUtil.findMethod(className1, funcName1, paramTypes1);
        Method method2 = ReflectUtil.findMethod(className2, funcName2, paramTypes2);
        boolean success = MethodHook.hookMethod1(method1, method2);
        Log.i(TAG, "hookMethod1 success=" + success);
        if (success) {
            return;
        }

        String decriptor1 = Type.getMethodDescriptor(returnType1, paramTypes1);
        String decriptor2 = Type.getMethodDescriptor(returnType2, paramTypes2);
        success = MethodHook.hookMethod2(className1, funcName1, decriptor1, isStatic1,
                                         className2, funcName2, decriptor2, isStatic2);
        Log.i(TAG, "hookMethod2 success=" + success);
    }
}

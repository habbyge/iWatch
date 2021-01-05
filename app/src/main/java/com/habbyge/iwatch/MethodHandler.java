package com.habbyge.iwatch;

import androidx.annotation.Keep;

import com.habbyge.iwatch.util.ReflectUtil;

import java.lang.reflect.Method;

/**
 * Created by habbyge on 2021/1/5.
 */
@Keep
final class MethodHandler {

    @Keep
    private static long backupMethodPtr = -1L; // 原方法地址
    @Keep
    private static Method srcMethod = null;

    void hook(String className1, String funcName1, Class<?>[] paramTypes1, boolean isStatic1,
              String className2, String funcName2, Class<?>[] paramTypes2, boolean isStatic2) {

        ReflectUtil.findMethod(className1, funcName1, );

        srcMethod = method1;

        MethodHook.hook();
    }
}

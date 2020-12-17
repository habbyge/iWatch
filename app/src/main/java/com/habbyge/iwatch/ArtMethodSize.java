package com.habbyge.iwatch;

import android.util.Log;

import androidx.annotation.Keep;

import java.lang.reflect.Method;

/**
 * func1、func2 两个方法必须是相邻的，用于计算 ArtMethod 大小
 */
@Keep
public class ArtMethodSize {

    @Keep
    public static void func1() {}

    @Keep
    public static void func2() {}

    public static void init(int sdkVersion) {
        try {
            Method method1 = ArtMethodSize.class.getDeclaredMethod("func1");
            method1.setAccessible(true);

            Method method2 = ArtMethodSize.class.getDeclaredMethod("func2");
            method2.setAccessible(true);

            MethodHook.init(sdkVersion, method1, method2);
        } catch (Exception e) {
            Log.i("iWatch.ArtMethodSize", "ArtMethodSize init crash: " + e.getMessage());
        }
    }
}

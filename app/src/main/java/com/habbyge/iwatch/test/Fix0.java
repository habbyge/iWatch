package com.habbyge.iwatch.test;

import android.util.Log;

public class Fix0 {
    private static final String TAG = "iWatch.Fix0";

    private static final String name = "Mango && Pidan";

    public static void printf_Hook(String text) {
        Log.i(TAG, "printf fix, name=" + name);
        new TestCase1().report(text);
    }
}

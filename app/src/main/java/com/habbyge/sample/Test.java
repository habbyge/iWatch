package com.habbyge.sample;

import android.util.Log;

public class Test {
    public static final String TAG = "iWatch.Test";

    public static String family = "I love my family !!!!!!";

    public static void print(String txt) {
        Log.d(TAG, "print: " + txt);
    }

    public static void print2(String txt) {
        Log.d(TAG, "print: " + txt);
    }

    public static String getFamily() {
        return family;
    }
}

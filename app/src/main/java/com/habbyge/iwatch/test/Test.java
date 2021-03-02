package com.habbyge.iwatch.test;

import android.util.Log;

import androidx.annotation.Keep;

@Keep
public class Test {
    public static final String TAG = "iWatch.Test";

    public void print(String txt) {
        Log.d(TAG, "print: " + txt);
    }
}

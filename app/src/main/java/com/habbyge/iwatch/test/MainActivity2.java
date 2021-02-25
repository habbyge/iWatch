package com.habbyge.iwatch.test;

import android.app.Activity;
import android.util.Log;

import androidx.annotation.Keep;

public class MainActivity2 extends Activity {
    private static final String TAG = "iWatch.MainActivity2";

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume......");
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    private void testLog(String text) {
        Log.d(TAG, "printf_hook: " + "l love my family !! "+ ", " + text);
    }

    @Keep
    @SuppressWarnings("all")
    private void printf_hook(String text) {
        testLog(text);
    }
}

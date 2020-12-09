package com.habbyge.iwatch.test;

import android.util.Log;
import android.view.View;

import com.habbyge.iwatch.HookManager;

public class TestClickListener implements View.OnClickListener {
    private static final String TAG = "iWatch.TestClickListener";

    public static void hook() {
        try {
            Class<?> clazz1 = Class.forName("com.habbyge.iwatch.MainActivity$5");
            Class<?> clazz2 = Class.forName("com.habbyge.iwatch.test.TestClickListener");

            HookManager.get().fixMethod(clazz1, "onClick", new Class<?>[] {View.class},
                                        clazz2, "onClick", new Class<?>[] {View.class});
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onClick(View v) {
        Log.i(TAG, "onClick, this=" + this.getClass().getName());
    }
}

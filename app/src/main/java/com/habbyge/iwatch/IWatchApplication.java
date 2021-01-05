package com.habbyge.iwatch;

import android.app.Application;
import android.util.Log;

public class IWatchApplication extends Application {
    private static final String TAG = "iWatch.IWatchApplication";

    @Override
    public void onCreate() {
        super.onCreate();
//        TestClickListener.hook();
//        TestInlineCase_Fix.fix();

        try {
            Class<?> srcClass = Class.forName("com.habbyge.iwatch.MainActivity", true, this.getClassLoader());
            Class<?> dstClass = Class.forName("com.habbyge.iwatch.test.MainActivity2", true, this.getClassLoader());

            // 测试用例1:
//            HookManager.get().fixMethod(
//                    srcClass, "printf", new Class<?>[]{String.class},
//                    dstClass, "printf_hook", new Class<?>[]{String.class});

            // 测试用例2:
            HookManager.get().fixMethod(srcClass, "onResume", null, dstClass, "onResume", null);
        } catch (Exception e) {
            Log.e(TAG, "hook fail: " + e.getMessage());
        }
    }
}

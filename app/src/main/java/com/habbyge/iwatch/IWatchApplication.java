package com.habbyge.iwatch;

import android.app.Application;

public class IWatchApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // 必须最先执行
        IWatch.init();

        // 测试用例:
        IWatch.hook("com.habbyge.iwatch.MainActivity", "onResume", null, void.class, false,
                    "com.habbyge.iwatch.test.MainActivity2", "onResume", null, void.class, false);
    }
}

package com.habbyge.iwatch;

import android.app.Application;

import com.habbyge.iwatch.patch.PatchManager;

public class IWatchApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // 必须最先执行
        PatchManager.getInstance().init(getApplicationContext(),
                                        "fa25df64dd46ee117fbb2527d2dc1cd82fd04e67");
        // 加载所有补丁
        PatchManager.getInstance().loadPatch();

        // 测试用例:
        PatchManager.getInstance().testFix(
                "com.habbyge.iwatch.MainActivity", "onResume", null, void.class, false,
                "com.habbyge.iwatch.test.MainActivity2", "onResume", null, void.class, false);
    }
}

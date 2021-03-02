package com.habbyge.sample;

import android.app.Application;
import android.util.Log;

import com.habbyge.iwatch.patch.PatchManager;

public class IWatchApplication extends Application {
    private static final String TAG = "iWatch.IWatchApplication";

    @Override
    public void onCreate() {
        super.onCreate();

        // 必须最先执行的初始化，加载所有补丁
        boolean initRet = PatchManager.getInstance().init(getApplicationContext(), "0.1", "1", true);
        if (!initRet) {
            Log.e(TAG, "onCreate, init failure !");
        }
    }
}

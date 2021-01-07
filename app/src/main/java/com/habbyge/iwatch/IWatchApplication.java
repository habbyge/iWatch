package com.habbyge.iwatch;

import android.app.Application;
import android.os.Environment;
import android.util.Log;

import com.habbyge.iwatch.patch.PatchManager;

import java.io.IOException;

public class IWatchApplication extends Application {
    private static final String TAG = "iWatch.IWatchApplication";

    private static final String IPATCH_PATH = "/out.ipatch";

    @Override
    public void onCreate() {
        super.onCreate();

        // 必须最先执行的初始化
        PatchManager.getInstance().init(getApplicationContext(),
                                        "fa25df64dd46ee117fbb2527d2dc1cd82fd04e67");
        // 加载所有补丁
        PatchManager.getInstance().loadPatch();

        // FIXME: 这是一个测试用例：
        try {
            // .apatch file path
            String patchPath = getApplicationContext().getExternalFilesDir(
                    Environment.DIRECTORY_DOCUMENTS) + IPATCH_PATH;

            PatchManager.getInstance().addPatch(patchPath);
            Log.d(TAG, "ipatch: " + patchPath + " added.");
        } catch (IOException e) {
            Log.e(TAG, "", e);
        }

        // FIXME: 这是一个测试用例：
        PatchManager.getInstance().testFix(
                "com.habbyge.iwatch.MainActivity", "onResume", null, void.class, false,
                "com.habbyge.iwatch.test.MainActivity2", "onResume", null, void.class, false);
    }
}

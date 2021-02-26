package com.habbyge.iwatch;

import android.app.Application;
import android.util.Log;

import com.habbyge.iwatch.patch.PatchManager;

public class IWatchApplication extends Application {
    private static final String TAG = "iWatch.IWatchApplication";

//    private static final String IPATCH_PATH = "/out.apatch";

    @Override
    public void onCreate() {
        super.onCreate();

        // 必须最先执行的初始化，加载所有补丁
        boolean initRet = PatchManager.getInstance().init(getApplicationContext(), "0.1", "1", true);
        if (!initRet) {
            Log.e(TAG, "onCreate, init failure !");
            return;
        }

        // 这是一个测试用例：
//        try {
//            // .apatch file path:
//            // /storage/emulated/0/Android/data/com.habbyge.iwatch/files/Documents/out.apatch
//            String patchPath = getApplicationContext().getExternalFilesDir(
//                    Environment.DIRECTORY_DOCUMENTS) + IPATCH_PATH;
//
//            PatchManager.getInstance().addPatch(patchPath);
//            Log.d(TAG, "ipatch: " + patchPath + " added.");
//        } catch (IOException e) {
//            Log.e(TAG, "out.apatch", e);
//        }

        // 这是一个测试用例：
//        PatchManager.getInstance().testFix(
//                "com.habbyge.iwatch.test.MainActivity", "onResume", null, void.class, false,
//                "com.habbyge.iwatch.test.MainActivity2", "onResume", null, void.class, false);
    }
}

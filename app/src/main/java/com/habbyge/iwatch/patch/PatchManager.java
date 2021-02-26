package com.habbyge.iwatch.patch;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;

import androidx.annotation.Nullable;

import com.habbyge.iwatch.IWatch;
import com.habbyge.iwatch.util.FileUtil;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Created by habbyge on 2021/1/5.
 */
public final class PatchManager {
    private static final String TAG = "iWatch.PatchManager";

    // patch extension
    private static final String SUFFIX = ".apatch"; // patch 文件的后缀
    private static final String DIR = "apatch";

    private String mIWatchVersion = null;
    private String mAppVersion = null;

    private File mPatchDir; // patch 目录: /data/user/0/com.habbyge.iwatch/files/apatch
    @Nullable
    private Patch mPatch;

    private IWatch mIWatch;

    @SuppressLint("StaticFieldLeak")
    private static PatchManager mInstance;

    public static PatchManager getInstance() {
        if (mInstance == null) {
            synchronized (PatchManager.class) {
                if (mInstance == null) {
                    mInstance = new PatchManager();
                }
            }
        }
        return mInstance;
    }

    private PatchManager() {
    }

    /**
     * 初始化入口(越早初始化越好)
     *
     * @param context 必须是全局的 Application 的 Context
     * @param iwatchVersion iwatch本身的版本号
     */
    public boolean init(Context context, String iwatchVersion, String appVersion) {
        mIWatchVersion = iwatchVersion;
        mAppVersion = appVersion;

        mPatchDir = new File(context.getFilesDir(), DIR);
        if (!mPatchDir.exists() && !mPatchDir.mkdirs()) {// make directory fail
            Log.e(TAG, "patch dir create error.");
            return false;
        } else if (!mPatchDir.isDirectory()) { // not directory
            Log.e(TAG, "mPatchDir delete result=" + mPatchDir.delete());
            return false;
        }
        Log.i(TAG, "mPatchDir=" + mPatchDir);

        initIWatch();

        initPatchs(); // 加载补丁到内存中: mPatchs

        return true;
    }

    /**
     * 实时加载补丁、生效函数，即：从server拉取到新的补丁后，就实施打补丁替换。
     *
     * 实时打补丁(add patch at runtime)，一般使用时机是：当该补丁下载到sdcard目录后，立马调用，即时生效.
     * When a new patch file has been downloaded, it will become effective immediately by addPatch.
     */
    public void addPatch(String patchPath) throws IOException {
        File newPatchFile = new File(patchPath);
        if (!newPatchFile.exists()) {
            return;
        }
        File destPathchFile = new File(mPatchDir, newPatchFile.getName());
        if (destPathchFile.exists()) {
            Log.w(TAG, "patch [" + patchPath + "] has be loaded.");
            return;
        }

        resetAllPatch(); // 清理带哦旧的path，重新load新的；恢复原始方法，重新hook新的方法

        FileUtil.copyFile(newPatchFile, destPathchFile); // copy to patch's directory
        FileUtil.deleteFile(newPatchFile); // 删除下载下来的patch文件
        boolean success = addPatch(destPathchFile);
        if (success && mPatch != null) {
            loadPatch(mPatch);
        }
    }

    private void initIWatch() {
        mIWatch = new IWatch();
        mIWatch.init();
    }

    /**
     * remove all patchs && resotore all origin methods.
     */
    private void resetAllPatch() {
        mIWatch.unhookAllMethod(); // 恢复原始函数
        cleanPatch();              // 删除所有补丁
    }

    /**
     * 冷启动打补丁
     */
    private void initPatchs() {
        File[] files = mPatchDir.listFiles();
        if (files == null || files.length <= 0) {
            Log.i(TAG, "initPatchs, failure: patch files is NULL !");
            return;
        }
        // 补丁 "从新到旧" 排序，只是用最新的补丁包，也就是说一个用户app只支持一个补丁，然后旧的补丁
        Arrays.sort(files, new Comparator<File>() {
            @Override
            public int compare(File f1, File f2) {
                return Long.compare(f2.lastModified(), f1.lastModified());
            }
        });

        boolean success = false;
        File file;
        for (int i = 0; i < files.length; ++i) {
            file = files[i];
            if (!file.exists()) {
                continue;
            }
            if (i == 0) { // files[0] 是最新修改的文件
                success = addPatch(file);
            } else { // 旧的补丁文件删掉之
                // noinspection ResultOfMethodCallIgnored
                file.delete();
            }
        }
        if (!success) {
            resetAllPatch();
            mPatch = null;
            Log.e(TAG, "initPatchs, failure: all patch files is illegal !");
            return;
        }

        boolean loadSuccess = loadPatch();
        Log.i(TAG, "initPatchs, success: " + loadSuccess);
    }

    /**
     * load all patch, call when application start，and fix all classes and methods
     */
    private boolean loadPatch() {
        if (mPatch == null) {
            return false;
        }
        List<String> classes = mPatch.getClasses();
        if (classes == null || classes.isEmpty()) {
            resetAllPatch();
            mPatch = null;
            return false;
        }
        mIWatch.fix(mPatch.getFile(), classes);
        return true;
    }

    /**
     * load specific patch
     * @param patch patch
     */
    private void loadPatch(Patch patch) {
        List<String> classes = patch.getClasses();
        if (classes == null || classes.isEmpty()) {
            resetAllPatch();
            mPatch = null;
            return;
        }
        mIWatch.fix(patch.getFile(), classes);
    }

    /**
     * add patch file
     */
    private boolean addPatch(File pathFile) {
        boolean succe = false;
        if (pathFile.getName().endsWith(SUFFIX)) {
            try {
                mPatch = new Patch(pathFile);
                if (!mPatch.canPatch()) {
                    resetAllPatch();
                    mPatch = null;

                    succe = false;
                } else {
                    succe = true;
                }
            } catch (IOException e) {
                Log.e(TAG, "addPatch", e);
                succe = false;
            }
        }
        return succe;
    }

    private void cleanPatch() {
        File[] files = mPatchDir.listFiles(); // 起始就一个 patch 文件
        if (files == null) {
            Log.e(TAG, "cleanPatch: files=null");
            return;
        }
        for (File file : files) {
            if (!FileUtil.deleteFile(file)) {
                Log.e(TAG, file.getName() + " delete error.");
            }
        }
    }

//    public void testFix(String className1, String funcName1, Class<?>[] paramTypes1,
//                        Class<?> returnType1, boolean isStatic1,
//                        String className2, String funcName2, Class<?>[] paramTypes2,
//                        Class<?> returnType2, boolean isStatic2) {
//
//        try {
//            Class<?> class2 = Class.forName(className2);
//            Method method2 = class2.getDeclaredMethod(funcName2, paramTypes2);
//            mIWatch.fixMethod1(PatchManager.class.getClassLoader(), className1, funcName1, method2);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }

//        try {
//            mIWatch.fixMethod2(className1, funcName1, paramTypes1, returnType1, isStatic1,
//                               className2, funcName2, paramTypes2, returnType2, isStatic2);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }

    public String getAppVersion() {
        return mAppVersion;
    }
}

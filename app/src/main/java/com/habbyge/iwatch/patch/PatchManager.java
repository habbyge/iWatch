package com.habbyge.iwatch.patch;

import android.content.Context;
import android.util.Log;

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

    private String mAppVersion = null;

    private File mPatchDir; // patch 目录: /data/user/0/com.habbyge.iwatch/files/apatch

    private Patch mPatch;
    private IWatch mIWatch;

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
     * @param open 方案开关
     */
    public boolean init(Context context, String iwatchVersion, String appVersion, boolean open) {
        if (!open) {
            Log.e(TAG, "__XYX__ init switcher is CLOSE !");
            return false;
        }

        mAppVersion = appVersion;
        // 现在是测试路径: context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
        mPatchDir = new File(context.getFilesDir(), Patch.DIR);

        if (!mPatchDir.exists() && !mPatchDir.mkdirs()) {// make directory fail
            Log.e(TAG, "patch dir create error.");
            return false;
        } else if (!mPatchDir.isDirectory()) { // not directory
            Log.e(TAG, "mPatchDir delete result=" + mPatchDir.delete());
            return false;
        }
        Log.i(TAG, "mPatchDir=" + mPatchDir + ", iwatchVersion=" + iwatchVersion);

        initIWatch();
        initPatchs();

        return true;
    }

    /**
     * 实时加载补丁、生效函数，即：从server拉取到新的补丁后，就实施打补丁替换。
     *
     * 实时打补丁(add patch at runtime)，一般使用时机是：当该补丁下载到sdcard目录后，立马调用，即时生效.
     * When a new patch file has been downloaded, it will become effective immediately by addPatch.
     * @param open iwatch方案的开关
     */
    public boolean addPatch(String patchPath, boolean open) throws IOException {
        if (!open) {
            Log.e(TAG, "__XYX__ addPatch switcher is CLOSE !");
            resetAllPatch(); // 清理掉旧的patch，重新load新的；恢复原始方法，重新hook新的方法
            return false;
        }

        File newPatchFile = new File(patchPath);
        if (!newPatchFile.exists()) {
            return false;
        }
        File destPathchFile = new File(mPatchDir, newPatchFile.getName());
        if (destPathchFile.exists()) {
            Log.w(TAG, "patch [" + patchPath + "] has be loaded.");
            return false;
        }

        resetAllPatch(); // 清理掉旧的patch，重新load新的；恢复原始方法，重新hook新的方法

        FileUtil.copyFile(newPatchFile, destPathchFile); // copy to patch's directory
        FileUtil.deleteFile(newPatchFile); // 删除下载下来的patch文件
        boolean success = addPatch(destPathchFile);
        if (success && mPatch != null) {
            return loadPatch(mPatch);
        }
        return false;
    }

    private void initIWatch() {
        mIWatch = new IWatch();
    }

    /**
     * remove all patchs && resotore all origin methods.
     * 恢复机制执行时机：
     * 1. iwatch开关-关闭
     * 2. host版本不符
     * 3. 获取的patch包信息异常
     */
    private void resetAllPatch() {
        mIWatch.reset(); // 恢复原始函数，清理掉缓存
        cleanPatch();    // 删除所有补丁
    }

    /**
     * 冷启动打补丁
     */
    private void initPatchs() {
        File[] files = mPatchDir.listFiles();
        if (files == null || files.length <= 0) {
            Log.e(TAG, "initPatchs, failure: patch files is NULL !");
            return;
        }
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

    private boolean loadPatch() {
        if (mPatch == null) {
            return false;
        }
        return loadPatch(mPatch);
    }

    private boolean loadPatch(Patch patch) {
        List<String> classes = patch.getClasses();
        if (classes == null || classes.isEmpty()) {
            resetAllPatch();
            mPatch = null;
            return false;
        }
        mIWatch.fix(patch.getFile(), classes);
        return true;
    }

    /**
     * add patch file
     */
    private boolean addPatch(File pathFile) {
        boolean succe = false;
        if (pathFile.getName().endsWith(Patch.SUFFIX)) {
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
        File[] files = mPatchDir.listFiles();
        if (files == null) {
            Log.w(TAG, "cleanPatch: files=null");
            return;
        }
        for (File file : files) {
            if (!FileUtil.deleteFile(file)) {
                Log.e(TAG, file.getName() + " delete error.");
            }
        }
    }

    public String getAppVersion() {
        return mAppVersion;
    }
}

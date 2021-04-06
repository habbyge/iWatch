package com.habbyge.iwatch.patch;

import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.Keep;

import com.habbyge.iwatch.IWatch;
import com.habbyge.iwatch.util.HellUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Created by habbyge on 2021/1/5.
 *
 * 1. patch下载的url，iWatch透明，让业务app自己处理，iWatch不做处理
 * 2. patch下载目录、存储目录，iWatch透明，且让业务app提供下载器，因为一般业务app都有自己的下载器(一般在下载前会做安全校验)
 *  这样做的目的是为了让业务app控制存储目录，满足业务app的存储管理要求，例如：微信对sdcard、/data目录都有非常严格的管理要求，
 *  还设计了vfs，管理文件目录、生命周期(失效、清理逻辑等)、大小等。iWatch需要服从业务app的这些要求。避免搞破坏。
 */
@Keep // 对外接口需要keep住
public final class PatchManager {
    private static final String TAG = "iWatch.PatchManager";

    private String mAppVersion = null;

    private Patch mPatch;
    private IWatch mWatch;

    private static PatchManager mInstance;

    @Keep
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
    @Keep
    public boolean init(String version, List<String> mBaseVerList, String appVersion, String patchPath, boolean open) {
        if (!open) {
            Log.e(TAG, "__XYX__ init switcher is CLOSE !");
            return false;
        }
        if (TextUtils.isEmpty(patchPath)) {
            Log.e(TAG, "__XYX__ init patchPath is NULL !");
            return false;
        }
        File patchFile = new File(patchPath);
        if (!patchFile.exists() || !patchFile.isFile()) {
            Log.e(TAG, "__XYX__ init patchFile is legal !");
            return false;
        }

        mAppVersion = appVersion;
        Log.i(TAG, "version=" + version + ", appVersion=" + appVersion + ", patchPath=" + patchPath);

        mWatch = new IWatch();

        return initPatchs(patchFile, mBaseVerList);
    }

    /**
     * 实时加载补丁、生效函数，即：从server拉取到新的补丁后，就实施打补丁替换。
     *
     * 实时打补丁(add patch at runtime)，一般使用时机是：当该补丁下载到sdcard目录后，立马调用，即时生效.
     * When a new patch file has been downloaded, it will become effective immediately by addPatch.
     * @param open iWatch 方案的开关
     */
    @Keep
    public boolean loadPatch(List<String> baseVerList, String appVersion, String patchPath, boolean open) {
        if (!open) {
            Log.e(TAG, "__XYX__ addPatch switcher is CLOSE !");
            resetAllPatch(); // 清理掉旧的patch，重新load新的；恢复原始方法，重新hook新的方法
            return false;
        }

        File patchFile = new File(patchPath);
        if (!patchFile.exists()) {
            return false;
        }

        resetAllPatch(); // 清理掉旧的patch，重新load新的；恢复原始方法，重新hook新的方法

        boolean success = addPatch(patchFile, baseVerList);
        if (success && mPatch != null) {
            return loadPatch(mPatch);
        }
        return false;
    }

    /**
     * remove all patchs && resotore all origin methods.
     * 恢复机制执行时机：
     * 1. iWatch开关-关闭
     * 2. host版本不符
     * 3. 获取的patch包信息异常
     */
    private void resetAllPatch() {
        mWatch.reset(); // 恢复原始函数，清理掉缓存
        cleanPatch();    // 删除所有补丁
    }

    /**
     * 冷启动打补丁
     */
    private boolean initPatchs(File patchFile, List<String> baseVerList) {
        boolean success = addPatch(patchFile, baseVerList);
        if (!success) {
            resetAllPatch();
            mPatch = null;
            Log.e(TAG, "initPatchs, failure: all patch files is illegal !");
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
        return mWatch.fix(patch.getFile(), classes);
    }

    /**
     * add patch file
     */
    private boolean addPatch(File pathFile, List<String> baseVerList) {
        boolean succe = false;
        if (pathFile.getName().endsWith(Patch.SUFFIX)) {
            try {
                mPatch = new Patch(pathFile, baseVerList);
                if (!mPatch.canPatch(mAppVersion)) {
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
        File file;
        if (mPatch != null && (file = mPatch.getFile()) != null) {
            if (!HellUtils.deleteFile(file)) {
                Log.e(TAG, file.getName() + " delete error.");
            }
        }
    }
}

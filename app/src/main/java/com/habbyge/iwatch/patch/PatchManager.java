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

// 需要注意:
// 1. 除了提供 hook 功能之外，还需要提供 一键恢复出厂设置 的功能，免得出大坑：只要补丁版本与Base App版本不匹配，
//    即恢复所有原始方法

// 2. iWatch 立项，还有两个理由：
//    理由1: 我们数据开发组接需求，是以周为单位，并不跟主线版本(包括tinker版本)，那么就必然需要一个 "热补丁" 来
//    实时发包，以达到跟你web/后台服务相同实时发布的功能，另外，还必须与 tinker 互斥；
//    理由2: 应对数据上报需求的碎片化：时间上、需求本身都很碎;

// 3. 需要考虑 不同上报需求提出的时间是碎片化的，因此补丁也可以是多个同时生效，此时考虑是一个app版本只使用一个补丁
//    好？还是一个app版本对应多个不同的补丁生效？
//    从 结果来看，"一对多" 更好，但是有个问题是，如果两个不同的上报需求，需要同时修改同一个函数，那么就会因为不知
//    道其他补丁的情况，而发生覆盖情况，因此从实用角度来看，"一对一" 模式更好。所以这里选择 "一对一"模式.
//    一个用户(微信App)只让一个补丁生效，不允许多补丁存在，是 "一对一" 的关系.

// 4. 需要考虑随时存在的 tinker 或 正常升级 导致的 微信 app 版本号改变情况，这个情况微信会重启，因此不会有问题.

/**
 * Created by habbyge on 2021/1/5.
 * patch manager
 *
 * Android 使用 PathClassLoader 作为其类加载器(加载已经安装到系统路径的apk包)，
 * DexClassLoader 可以从 .jar 和 .apk 类型的文件内部加载 classes.dex文件.
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
     * @param patchPath path
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
//            mIWatch.removeOptFile(file);
            if (!FileUtil.deleteFile(file)) {
                Log.e(TAG, file.getName() + " delete error.");
            }
        }
    }

//    public void testFix(String className1, String funcName1, Class<?>[] paramTypes1,
//                        Class<?> returnType1, boolean isStatic1,
//                        String className2, String funcName2, Class<?>[] paramTypes2,
//                        Class<?> returnType2, boolean isStatic2) {

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

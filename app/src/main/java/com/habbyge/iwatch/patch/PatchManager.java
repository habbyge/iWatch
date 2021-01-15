package com.habbyge.iwatch.patch;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;

import androidx.annotation.Nullable;

import com.habbyge.iwatch.IWatch;
import com.habbyge.iwatch.util.FileUtil;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;

// TODO: 除了提供 hook 功能之外，还需要提供 一键恢复出厂设置 的功能，免得出大坑。

// TODO: iWatch 立项，还有一个理由：我们数据开发组接需求，是以周为单位，并不跟主线版本(包括tinker版本)，
//  那么就必然需要一个 "热补丁" 来实时发包，另外，还必须与 tinker 互斥；

// TODO: 1/15/21 需要考虑 不同上报需求提出的时间是碎片化的，因此补丁也可以是多个同时生效，此时考虑是一个app版本
//  只使用一个补丁好？还是一个app版本对应多个不同的补丁生效？
//  从 结果来看，"一对多" 更好，但是有个问题是，如果两个不同的上报需求，需要同时修改同一个函数，那么就会因为不知道其他
//  补丁的情况，而发生覆盖情况，因此从实用角度来看，"一对一" 模式更好。所以这里选择 "一对一"模式.
//  一个用户(微信App)只让一个补丁生效，不允许多补丁存在，是 "一对一" 的关系.

// TODO: 需要考虑随时存在的 tinker 或 正常升级 导致的 微信 app 版本号改变情况，这个情况微信会重启，因此不会有问题.

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
    private static final String SUFFIX = ".ipatch"; // patch 文件的后缀
    private static final String DIR = "ipatch";

    private Context mContext; // 这里必须是 Application 的 Context

    private String mIWatchVersion = null;
    private String mAppVersion = null;

    private File mPatchDir; // patch 目录: /data/user/0/com.habbyge.iwatch/files/ipatch
    @Nullable
    private Patch mPatch;

    private ClassLoader mClassLoader;

    private IWatch iWatch;

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
        mContext = context;
        mIWatchVersion = iwatchVersion;
        mAppVersion = appVersion;

        mPatchDir = new File(mContext.getFilesDir(), DIR);
        if (!mPatchDir.exists() && !mPatchDir.mkdirs()) {// make directory fail
            Log.e(TAG, "patch dir create error.");
            return false;
        } else if (!mPatchDir.isDirectory()) { // not directory
            Log.e(TAG, "mPatchDir delete result=" + mPatchDir.delete());
            return false;
        }
        Log.i(TAG, "mPatchDir=" + mPatchDir);

        initIWatch(context);

        initPatchs(); // 加载补丁到内存中: mPatchs

        return true;
    }

    /**
     * load all patch, call when application start，and fix all classes and methods
     */
    public void loadPatch() {
        if (mPatch == null || !mPatch.canPatch()) {
            resetAllPatch();
            return;
        }

        mClassLoader = mContext.getClassLoader();

        List<String> classes;
        Set<String> patchNames = mPatch.getPatchNames();
        for (String patchName : patchNames) {
            classes = mPatch.getClasses(patchName);
            iWatch.fix(mPatch.getFile(), mContext.getClassLoader(), classes);
        }
    }

    /**
     * 实时加载补丁、生效函数接口
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

    /**
     * load patch, call when plugin be loaded. used for plugin architecture.</br>
     * <p>
     * need name and classloader of the plugin
     *
     * @param patchName   patch name
     * @param classLoader classloader
     */
    public void loadPatch(String patchName, ClassLoader classLoader) {
        if (mPatch == null || !mPatch.canPatch()) {
            resetAllPatch();
            return;
        }

        mClassLoader = classLoader;
        List<String> classes; // 该补丁文件(patchName)中所有的class
        Set<String> patchNames = mPatch.getPatchNames();
        if (patchNames.contains(patchName)) {
            classes = mPatch.getClasses(patchName);
            iWatch.fix(mPatch.getFile(), classLoader, classes);
        }
    }

    private void initIWatch(Context context) {
        iWatch = new IWatch(context);
        iWatch.init();
    }

    /**
     * remove all patchs && resotore all origin methods.
     */
    private void resetAllPatch() {
        iWatch.unhookAllMethod(); // 恢复原始函数
        cleanPatch();             // 删除所有补丁
    }

    private void initPatchs() {
        File[] files = mPatchDir.listFiles();
        if (files == null) {
            return;
        }
        boolean success = false;
        for (File file : files) {
            success = addPatch(file);
            if (!success) {
                break;
            }
        }
        Log.i(TAG, "initPatchs result=" + success);
    }

    /**
     * load specific patch
     * @param patch patch
     */
    private void loadPatch(Patch patch) {
        Set<String> patchNames = patch.getPatchNames();
        ClassLoader classLoader;
        List<String> classes;
        for (String patchName : patchNames) {
            if (mClassLoader == null) {
                classLoader = mContext.getClassLoader();
            } else {
                classLoader = mClassLoader;
            }
            if (classLoader != null) {
                classes = patch.getClasses(patchName);
                iWatch.fix(patch.getFile(), classLoader, classes);
            }
        }
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
            iWatch.removeOptFile(file);
            if (!FileUtil.deleteFile(file)) {
                Log.e(TAG, file.getName() + " delete error.");
            }
        }
    }

    public void testFix(String className1, String funcName1, Class<?>[] paramTypes1,
                        Class<?> returnType1, boolean isStatic1,
                        String className2, String funcName2, Class<?>[] paramTypes2,
                        Class<?> returnType2, boolean isStatic2) {

        iWatch.hook(className1, funcName1, paramTypes1, returnType1, isStatic1,
                    className2, funcName2, paramTypes2, returnType2, isStatic2);
    }

    public String getAppVersion() {
        return mAppVersion;
    }
}

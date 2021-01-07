package com.habbyge.iwatch.patch;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.habbyge.iwatch.IWatch;
import com.habbyge.iwatch.util.FileUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

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
    private static final String SUFFIX = ".ipatch"; // patch文件的后缀
    private static final String DIR = "ipatch";
    private static final String SP_NAME = "_iwatch_";
    private static final String SP_REVERSION = "ipatch_reversion";

    private Context mContext; // 这里必须是 Application 的 Context

    private File mPatchDir; // patch 目录
    private SortedSet<Patch> mPatchs; // TODO: 1/7/21 保存了所有补丁
    /**
     * classloaders
     */
    private Map<String, ClassLoader> mClassLoaderMap; // TODO: 1/7/21 ing

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
     * @param appReversion App 打包粒度的 version
     */
    public void init(Context context, String appReversion) {
        mContext = context;

        mPatchDir = new File(mContext.getFilesDir(), DIR);
        if (!mPatchDir.exists() && !mPatchDir.mkdirs()) {// make directory fail
            Log.e(TAG, "patch dir create error.");
            return;
        } else if (!mPatchDir.isDirectory()) { // not directory
            Log.e(TAG, "mPatchDir delete result=" + mPatchDir.delete());
            return;
        }

        // 线程安全的有序的集合，适用于高并发的场景
        mPatchs = new ConcurrentSkipListSet<Patch>();
        mClassLoaderMap = new ConcurrentHashMap<String, ClassLoader>();

        SharedPreferences sp = mContext.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        String reversion = sp.getString(SP_REVERSION, null);
        if (reversion == null || !reversion.equalsIgnoreCase(appReversion)) {
            cleanPatch(); // 非对应 reversion 版本，则清理掉补丁，使用原始逻辑
            sp.edit().putString(SP_REVERSION, appReversion).apply();
            Log.e(TAG, "PatchManager init failure appReversion=" + appReversion);
        } else {
            initPatchs();
        }

        initIWatch(context);
    }

    /**
     * load all patch, call when application start
     */
    public void loadPatch() {
        mClassLoaderMap.put("*", mContext.getClassLoader()); // wildcard
        Set<String> patchNames;
        List<String> classes;
        for (Patch patch : mPatchs) {
            patchNames = patch.getPatchNames();
            for (String patchName : patchNames) {
                classes = patch.getClasses(patchName);
                iWatch.fix(patch.getFile(), mContext.getClassLoader(), classes);
            }
        }
    }

    /**
     * 实时打补丁(add patch at runtime)，一般使用时机是：当该补丁下载到sdcard目录后，立马调用，即时生效.
     * When a new patch file has been downloaded, it will become effective immediately by addPatch.
     * @param path patch path
     */
    public void addPatch(String path) throws IOException {
        File src = new File(path);
        File dest = new File(mPatchDir, src.getName());
        if (!src.exists()) {
            Log.e(TAG, "addPath, FileNotFoundException", new FileNotFoundException(path));
            return;
        }
        if (dest.exists()) {
            Log.d(TAG, "patch [" + path + "] has be loaded.");
            return;
        }
        FileUtil.copyFile(src, dest); // copy to patch's directory
        Patch patch = addPatch(dest);
        if (patch != null) {
            loadPatch(patch);
        }
    }

    /**
     * load patch,call when plugin be loaded. used for plugin architecture.</br>
     * <p>
     * need name and classloader of the plugin
     *
     * @param patchName   patch name
     * @param classLoader classloader
     */
    public void loadPatch(String patchName, ClassLoader classLoader) {
        mClassLoaderMap.put(patchName, classLoader);
        Set<String> patchNames;
        List<String> classes; // 该补丁文件(patchName)中所有的class
        for (Patch patch : mPatchs) {
            patchNames = patch.getPatchNames();
            if (patchNames.contains(patchName)) {
                classes = patch.getClasses(patchName);
                iWatch.fix(patch.getFile(), classLoader, classes);
            }
        }
    }

    /**
     * remove all patchs
     */
    public void removeAllPatch() {
        cleanPatch();
        SharedPreferences sp = mContext.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        sp.edit().clear().apply();
    }

    private void initPatchs() {
        File[] files = mPatchDir.listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            addPatch(file);
        }
        Log.i(TAG, "initPatchs success");
    }

    private void initIWatch(Context context) {
        iWatch = new IWatch(context);
        iWatch.init();
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
            if (mClassLoaderMap.containsKey("*")) {
                classLoader = mContext.getClassLoader();
            } else {
                classLoader = mClassLoaderMap.get(patchName);
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
    private Patch addPatch(File pathFile) {
        Patch patch = null;
        if (pathFile.getName().endsWith(SUFFIX)) {
            try {
                patch = new Patch(pathFile);
                mPatchs.add(patch);
            } catch (IOException e) {
                Log.e(TAG, "addPatch", e);
            }
        }
        return patch;
    }

    private void cleanPatch() {
        File[] files = mPatchDir.listFiles();
        if (files == null) {
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
}

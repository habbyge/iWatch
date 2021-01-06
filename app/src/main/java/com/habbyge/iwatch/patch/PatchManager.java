package com.habbyge.iwatch.patch;

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
    private static final String SP_VERSION = "ipatch_reversion";

    private final Context mContext; // 这里必须是 Application 的 Context

    /**
     * patch directory
     */
    private final File mPatchDir;
    /**
     * patchs
     */
    private final SortedSet<Patch> mPatchs;
    /**
     * classloaders
     */
    private final Map<String, ClassLoader> mClassLoaderMap;

    private final IWatch iWatch;

    /**
     * @param context context
     */
    public PatchManager(Context context) {
        mContext = context;
        mPatchDir = new File(mContext.getFilesDir(), DIR);
        iWatch = new IWatch(context);
        // 线程安全的有序的集合，适用于高并发的场景
        mPatchs = new ConcurrentSkipListSet<Patch>();
        mClassLoaderMap = new ConcurrentHashMap<String, ClassLoader>();
    }

    /**
     * @param appReversion App 打包粒度的 version
     */
    public void init(String appReversion) {
        if (!mPatchDir.exists() && !mPatchDir.mkdirs()) {// make directory fail
            Log.e(TAG, "patch dir create error.");
            return;
        } else if (!mPatchDir.isDirectory()) { // not directory
            Log.e(TAG, "mPatchDir delete result=" + mPatchDir.delete());
            return;
        }

        SharedPreferences sp = mContext.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        String reversion = sp.getString(SP_VERSION, null);
        if (reversion == null || !reversion.equalsIgnoreCase(appReversion)) {
            cleanPatch(); // 非对应 reversion 版本，则清理掉补丁，使用原始逻辑 TODO: ing......
            sp.edit().putString(SP_VERSION, appReversion).apply();
        } else {
            initPatchs();
        }
    }

    private void initPatchs() {
        File[] files = mPatchDir.listFiles();
        for (File file : files) {
            addPatch(file);
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
        for (File file : files) {
            iWatch.removeOptFile(file);
            if (!FileUtil.deleteFile(file)) {
                Log.e(TAG, file.getName() + " delete error.");
            }
        }
    }

    /**
     * 实时打补丁的接口函数
     * add patch at runtime
     * @param path patch path
     */
    @SuppressWarnings("unused")
    public void addPatch(String path) throws IOException {
        File src = new File(path);
        File dest = new File(mPatchDir, src.getName());
        if (!src.exists()) {
            throw new FileNotFoundException(path);
        }
        if (dest.exists()) {
            Log.d(TAG, "patch [" + path + "] has be loaded.");
            return;
        }
        FileUtil.copyFile(src, dest);// copy to patch's directory
        Patch patch = addPatch(dest);
        if (patch != null) {
            loadPatch(patch);
        }
    }

    /**
     * remove all patchs
     */
    @SuppressWarnings("unused")
    public void removeAllPatch() {
        cleanPatch();
        SharedPreferences sp = mContext.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        sp.edit().clear().apply();
    }

    /**
     * load patch,call when plugin be loaded. used for plugin architecture.</br>
     * <p>
     * need name and classloader of the plugin
     *
     * @param patchName   patch name
     * @param classLoader classloader
     */
    @SuppressWarnings("unused")
    public void loadPatch(String patchName, ClassLoader classLoader) {
        mClassLoaderMap.put(patchName, classLoader);
        Set<String> patchNames;
        List<String> classes;
        for (Patch patch : mPatchs) {
            patchNames = patch.getPatchNames();
            if (patchNames.contains(patchName)) {
                classes = patch.getClasses(patchName);
                mAndFixManager.fix(patch.getFile(), classLoader, classes);
            }
        }
    }

    /**
     * load patch, call when application start
     */
    @SuppressWarnings("unused")
    public void loadPatch() {
        mClassLoaderMap.put("*", mContext.getClassLoader());// wildcard
        Set<String> patchNames;
        List<String> classes;
        for (Patch patch : mPatchs) {
            patchNames = patch.getPatchNames();
            for (String patchName : patchNames) {
                classes = patch.getClasses(patchName);
                mAndFixManager.fix(patch.getFile(), mContext.getClassLoader(), classes);
            }
        }
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
                mAndFixManager.fix(patch.getFile(), classLoader, classes);
            }
        }
    }
}


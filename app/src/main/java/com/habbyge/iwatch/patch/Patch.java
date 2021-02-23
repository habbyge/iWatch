package com.habbyge.iwatch.patch;

import android.text.TextUtils;

import androidx.annotation.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * Created by habbyge on 2021/1/5.
 */
public class Patch implements Comparable<Patch> {
    private static final String ENTRY_NAME = "META-INF/PATCH.MF";
    private static final String PATCH_CLASSES = "Patch-Classes";
    private static final String CREATED_TIME = "Created-Time";
    private static final String PATCH_NAME = "Patch-Name";

    private static final String PATCH_VERSION = "Patch-Version";
    private static final String BASE_APP_VERSION = "Base-App-Version";

    private final File mFile;
    private String mName;
    private Date mTime;
    private String mPatchVersion;
    private String mBaseAppVersion;

    // 该 Patch 中包含的所有Class
    List<String> mClasses = null;

    // TODO: 1/21/21 如何加载补丁中的class：
    //  通过构造 补丁apk 中的 DexClassLoader 来加载 补丁apk 中的类；
    //  DexClassLoader 的 parent 设置为 宿主程序 的 classloader，即可将 宿主程序 和 补丁程序 中的 class 贯通；
    //  这里：生成 patch.apk 包时，会把补丁包中需要修改的类，改成_CF结尾的类名，所以不会有重复类。
    private ClassLoader mClassLoader;

    public Patch(File file) throws IOException {
        mFile = file;
        init();
    }

    /**
     * Patch 包解析构造成 Patch 类对象时，主要是解析 patch 包中的 META-INF/PATCH.MF 文件，将该文件中
     * Patch-Classes 项对应的值解析出来，在前面 apkpatch 工具实现分析一文中有介绍，这个字段对应的值是将
     * 修复的 Class 以英文逗号分隔组成。
     */
    private void init() throws IOException {
        JarFile jarFile = null;
        InputStream inputStream = null;
        try {
            jarFile = new JarFile(mFile);
            JarEntry entry = jarFile.getJarEntry(ENTRY_NAME);
            inputStream = jarFile.getInputStream(entry);
            Manifest manifest = new Manifest(inputStream);
            Attributes mainAttributes = manifest.getMainAttributes();
            mName = mainAttributes.getValue(PATCH_NAME);
            /*mTime = new Date(mainAttributes.getValue(CREATED_TIME));*/
            mTime = DateFormat.getTimeInstance().parse(mainAttributes.getValue(CREATED_TIME));

            mPatchVersion = mainAttributes.getValue(PATCH_VERSION);
            mBaseAppVersion = mainAttributes.getValue(BASE_APP_VERSION);

            Attributes.Name attrName;
            String name;
            for (Object attr : mainAttributes.keySet()) {
                attrName = ((Attributes.Name) attr);
                name = attrName.toString();
                if (PATCH_CLASSES.equalsIgnoreCase(name)) {
                    mClasses = Arrays.asList(mainAttributes.getValue(attrName).split(","));
                    break;
                }
            }
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        } finally {
            if (jarFile != null) {
                jarFile.close();
            }
            if (inputStream != null) {
                inputStream.close();
            }
        }
    }

    public String getName() {
        return mName;
    }

    public File getFile() {
        return mFile;
    }

    @Nullable
    public List<String> getClasses() {
        return mClasses;
    }

    public Date getTime() {
        return mTime;
    }

    @Override
    public int compareTo(Patch another) {
        return mTime.compareTo(another.getTime());
    }

    public boolean canPatch() {
        String appVersion = PatchManager.getInstance().getAppVersion();
        if (TextUtils.isEmpty(appVersion)) {
            return false;
        }
        return appVersion.equals(mBaseAppVersion);
    }
}

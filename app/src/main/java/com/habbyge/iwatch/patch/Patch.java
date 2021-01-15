package com.habbyge.iwatch.patch;

import android.text.TextUtils;

import androidx.annotation.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * Created by habbyge on 2021/1/5.
 */
public class Patch implements Comparable<Patch> {
    private static final String ENTRY_NAME = "META-INF/PATCH.MF";
    private static final String CLASSES = "-Classes";
    private static final String PATCH_CLASSES = "Patch-Classes";
    private static final String CREATED_TIME = "Created-Time";
    private static final String PATCH_NAME = "Patch-Name";

    private static final String PATCH_VERSION = "Patch-Version";
    private static final String BASE_APP_VERSION = "Base-App-Version";

    private final File mFile;
    private String mName;
    private Date mTime;
    private String mVersion;
    private String mBaseAppVersion;

    /**
     * classes of patch: <PatchName, List<该patch文件中包括的所有class文件名>>
     */
    private Map<String, List<String>> mClassesMap;

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
            mTime = new Date(mainAttributes.getValue(CREATED_TIME));

            mVersion = mainAttributes.getValue(PATCH_VERSION);
            mBaseAppVersion = mainAttributes.getValue(BASE_APP_VERSION);

            mClassesMap = new HashMap<String, List<String>>();
            Attributes.Name attrName;
            String name;
            List<String> classes;
            for (Object attr : mainAttributes.keySet()) {
                attrName = (Attributes.Name) attr;
                name = attrName.toString();
                if (name.endsWith(CLASSES)) {
                    classes = Arrays.asList(mainAttributes.getValue(attrName).split(","));
                    if (name.equalsIgnoreCase(PATCH_CLASSES)) {
                        mClassesMap.put(mName, classes);
                    } else {
                        mClassesMap.put(
                                name.trim().substring(0, name.length() - 8),// remove
                                classes); // "-Classes"
                    }
                }
            }
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

    public Set<String> getPatchNames() {
        return mClassesMap.keySet();
    }

    public List<String> getClasses(String patchName) {
        return mClassesMap.get(patchName);
    }

    public Date getTime() {
        return mTime;
    }

    @Nullable
    public String getPatchVersion() {
        return mVersion;
    }

    /**
     * @return 该 patch 支持的 app 版本，即该补丁是基于哪个基线版本打出来的
     */
    @Nullable
    public String getBaseAppVersion() {
        return mBaseAppVersion;
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

package com.habbyge.iwatch.patch;

import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * Created by habbyge on 2021/1/5.
 */
public class Patch {
    private static final String TAG = "iWatch.Patch";

    private static final String ENTRY_NAME = "META-INF/PATCH.MF";
    private static final String PATCH_CLASSES = "Patch-Classes"; // 包括新增、修改的类

    private final List<String> mBaseVerList = new ArrayList<>();

    // patch extension
    public static final String SUFFIX = ".apatch"; // patch 文件的后缀

    private final File mFile; // patch 文件名是固定的，也就是路径是固定的

    // 该 Patch 中包含的所有Class
    List<String> mClasses = null;

    public Patch(File file, List<String> baseVerList) throws IOException {
        mFile = file;
        mBaseVerList.clear();
        mBaseVerList.addAll(baseVerList);
        init();
    }

    /**
     * Patch 包解析构造成 Patch 类对象时，主要是解析 patch 包中的 META-INF/PATCH.MF 文件，将该文件中
     * Patch-Classes 项对应的值解析出来，在前面 apkpatch 工具实现分析一文中有介绍，这个字段对应的值是将
     * 修复的 Class 以英文逗号分隔组成。
     *
     * META-INF/PATCH.MF 中内容：
     * Manifest-Version: 1.0
     * Patch-Name: app-release-2
     * Created-Time: 26 Feb 2021 07:10:21 GMT
     * From-File: app-release-2.apk
     * To-File: app-release-1.apk
     * Patch-Classes: com.tencent.mm.iwatch.sample.MainActivity$4_CF,com.tencent.mm.iwatch.MainActivity$5_CF
     * Created-By: 1.0 (ApkPatch)
     *
     * 其中: "From-File" 表示: 修复包名-补丁版本号.apk "To-File" 表示: 基础包名-版本号.apk
     */
    private void init() throws IOException {
        JarFile jarFile = null;
        InputStream is = null;
        try {
            jarFile = new JarFile(mFile);
            JarEntry entry = jarFile.getJarEntry(ENTRY_NAME);
            is = jarFile.getInputStream(entry);
            Manifest manifest = new Manifest(is);
            Attributes mainAttributes = manifest.getMainAttributes();
            Attributes.Name attrName;
            String name;
            for (Object attr : mainAttributes.keySet()) {
                attrName = ((Attributes.Name) attr);
                name = attrName.toString();
                if (PATCH_CLASSES.equalsIgnoreCase(name)) {
                    String attrValue = mainAttributes.getValue(attrName);
                    Log.i(TAG, "init, Patch-Classes=" + attrValue);
                    mClasses = Arrays.asList(attrValue.split(","));
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (jarFile != null) {
                jarFile.close();
            }
            if (is != null) {
                is.close();
            }
        }
    }

    public File getFile() {
        return mFile;
    }

    public List<String> getClasses() {
        return mClasses;
    }

    public boolean canPatch(String appVersion) {
        if (TextUtils.isEmpty(appVersion)) {
            return false;
        }
        return mBaseVerList.contains(appVersion);
    }
}

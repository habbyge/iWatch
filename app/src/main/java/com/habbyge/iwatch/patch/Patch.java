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

    public static final String SUFFIX = ".apatch";
    private final File mFile;
    List<String> mClasses = null;

    public Patch(File file, List<String> baseVerList) throws IOException {
        mFile = file;
        mBaseVerList.clear();
        mBaseVerList.addAll(baseVerList);
        init();
    }
    
    private void init() throws IOException {
        JarFile jarFile = null;
        InputStream is = null;
        try {
            jarFile = new JarFile(mFile);
            JarEntry entry = jarFile.getJarEntry(ENTRY_NAME);
            is = jarFile.getInputStream(entry);
            Manifest manifest = new Manifest(is);
            Attributes attrs = manifest.getMainAttributes();
            Attributes.Name attrName;
            for (Object attr : attrs.keySet()) {
                attrName = ((Attributes.Name) attr);
                if (PATCH_CLASSES.equalsIgnoreCase(attrName.toString())) {
                    String attrValue = attrs.getValue(attrName);
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

package com.habbyge.iwatch.patch;

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

    /**
     * patch file
     */
    private final File mFile;
    /**
     * name
     */
    private String mName;
    /**
     * create time
     */
    private Date mTime;
    /**
     * classes of patch: <patchName, List<该patch文件中包括的所有class文件名>>
     */
    private Map<String, List<String>> mClassesMap;

    public Patch(File file) throws IOException {
        mFile = file;
        init();
    }

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

            mClassesMap = new HashMap<String, List<String>>();
            Attributes.Name attrName;
            String name;
            List<String> strings;
            for (Object attr : mainAttributes.keySet()) {
                attrName = (Attributes.Name) attr;
                name = attrName.toString();
                if (name.endsWith(CLASSES)) {
                    strings = Arrays.asList(mainAttributes.getValue(attrName).split(","));
                    if (name.equalsIgnoreCase(PATCH_CLASSES)) {
                        mClassesMap.put(mName, strings);
                    } else {
                        mClassesMap.put(
                                name.trim().substring(0, name.length() - 8),// remove
                                strings); // "-Classes"
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

    @Override
    public int compareTo(Patch another) {
        return mTime.compareTo(another.getTime());
    }
}


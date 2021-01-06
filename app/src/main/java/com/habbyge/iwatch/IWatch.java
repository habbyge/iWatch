package com.habbyge.iwatch;

import android.content.Context;
import android.util.Log;

import androidx.annotation.Keep;

import com.habbyge.iwatch.loader.SecurityChecker;
import com.habbyge.iwatch.util.ReflectUtil;
import com.habbyge.iwatch.util.StringUtil;
import com.habbyge.iwatch.util.Type;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import dalvik.system.DexFile;

/**
 * Created by habbyge on 2021/1/5.
 */
@Keep
public final class IWatch {
    private static final String TAG = "IWatch";

    private static final String DIR = "ipatch_opt";

    /**
     * context
     */
    private final Context mContext;

    /**
     * classes will be fixed
     */
    private static final Map<String, Class<?>> mFixedClass = new ConcurrentHashMap<>();

    /**
     * security check
     */
    private final SecurityChecker mSecurityChecker;

    /**
     * optimize directory
     */
    private final File mOptDir;

    public IWatch(Context context) {
        mContext = context;

        mSecurityChecker = new SecurityChecker(mContext);
        mOptDir = new File(mContext.getFilesDir(), DIR);
        if (!mOptDir.exists() && !mOptDir.mkdirs()) { // make directory fail
            Log.e(TAG, "opt dir create error.");
        } else if (!mOptDir.isDirectory()) {// not directory
            boolean ret = mOptDir.delete();
            Log.i(TAG, "mOptDir.delete(): " + ret);
        }
    }

    public void init() {
        MethodHook.init();
    }

    /**
     * fix
     * @param patchPath patch path
     */
    @SuppressWarnings("unused")
    public synchronized void fix(String patchPath) {
        doFix(new File(patchPath), mContext.getClassLoader(), null);
    }

    /**
     * FIX
     *
     * @param pathFile
     *            patch file
     * @param classLoader
     *            classloader of class that will be fixed
     * @param classNames
     *            classes will be fixed
     */
    public synchronized void fix(File pathFile, ClassLoader classLoader, List<String> classNames) {
        doFix(pathFile, classLoader, classNames);
    }

    private void doFix(File pathFile, ClassLoader classLoader, List<String> classNames) {
        if (!mSecurityChecker.verifyApk(pathFile)) { // security check fail
            return;
        }

        try {
            File optfile = new File(mOptDir, pathFile.getName());
            boolean saveFingerprint = true;
            if (optfile.exists()) {
                // need to verify fingerprint when the optimize file exist,
                // prevent someone attack on jailbreak device with
                // Vulnerability-Parasyte.
                // btw:exaggerated android Vulnerability-Parasyte
                // http://secauo.com/Exaggerated-Android-Vulnerability-Parasyte.html

                if (mSecurityChecker.verifyOpt(optfile)) {
                    saveFingerprint = false;
                } else if (!optfile.delete()) {
                    return;
                }
            }

            // libcore/dalvik/src/main/java/dalvik/system/DexFile.java
            final DexFile dexFile = DexFile.loadDex(pathFile.getAbsolutePath(),
                                                    optfile.getAbsolutePath(),
                                                    Context.MODE_PRIVATE);

            if (saveFingerprint) {
                mSecurityChecker.saveOptSig(optfile);
            }

            // 双亲机制，这里也是关键点之1/2，classLoader 决定的是该 补丁.apk 中被加载到内存中的class，
            // 是否能够被原apk识别。
            ClassLoader patchClassLoader = new ClassLoader(classLoader) {

                @Override
                protected Class<?> findClass(String className) throws ClassNotFoundException {
                    Class<?> clazz = dexFile.loadClass(className, this);
                    final String packagePath = "com.habbyge.iwatch";
                    if (clazz == null && className.startsWith(packagePath)) {
                        return Class.forName(className);// annotation’s class
                        // not found
                    }
                    if (clazz == null) {
                        throw new ClassNotFoundException(className);
                    }
                    Log.i(TAG, "patchClassLoader, fincClass=" + clazz.getName());
                    return clazz;
                }
            };

            Enumeration<String> entrys = dexFile.entries();
            Class<?> clazz;
            while (entrys.hasMoreElements()) {
                String entry = entrys.nextElement();
                if (classNames != null && !classNames.contains(entry)) {
                    continue; // skip, not need fix
                }
                clazz = dexFile.loadClass(entry, patchClassLoader);
                if (clazz != null) {
                    fixClass(clazz, classLoader);
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "pacth", e);
        }
    }

    /**
     * fix class
     * @param clazz class
     */
    private void fixClass(Class<?> clazz, ClassLoader classLoader) {
        Method[] methods = clazz.getDeclaredMethods();
        MethodReplace methodReplace;
        String className;
        String methodName;
        for (Method method : methods) {
            methodReplace = method.getAnnotation(MethodReplace.class);
            if (methodReplace == null) {
                continue;
            }
            className = methodReplace.clazz();
            methodName = methodReplace.method();
            if (!isEmpty(className) && !isEmpty(methodName)) {
                replaceMethod(classLoader, className, methodName, method);
            }
        }
    }

    /**
     * replace method
     *
     * @param classLoader classloader
     * @param className name of target class
     * @param methodname name of target method
     * @param srcMethod source method
     */
    private void replaceMethod(ClassLoader classLoader, String className,
                               String methodname, Method srcMethod) {

        try {
            String key = className + "@" + classLoader.toString();
            Class<?> clazz = mFixedClass.get(key);
            if (clazz == null) { // class not load
                Class<?> clzz = classLoader.loadClass(className);
                // initialize target class
                clazz = AndFix.initTargetClass(clzz);
            }
            if (clazz != null) { // initialize class OK
                mFixedClass.put(key, clazz);
                Method src = clazz.getDeclaredMethod(methodname, srcMethod.getParameterTypes());
                AndFix.addReplaceMethod(src, srcMethod); // 前者 替换 后者
            }
        } catch (Exception e) {
            Log.e(TAG, "replaceMethod", e);
        }
    }

    private static boolean isEmpty(String string) {
        return string == null || string.length() <= 0;
    }

    public void hook(String className1, String funcName1, Class<?>[] paramTypes1,
                     Class<?> returnType1, boolean isStatic1,
                     String className2, String funcName2, Class<?>[] paramTypes2,
                     Class<?> returnType2, boolean isStatic2) {

        if (StringUtil.isEmpty(className1) || StringUtil.isEmpty(funcName1)
                || StringUtil.isEmpty(className2) || StringUtil.isEmpty(funcName2)
                || returnType1 == null || returnType2 == null) {

            throw new NullPointerException("IWatch.hook, param is null");
        }

        Method method1 = ReflectUtil.findMethod(className1, funcName1, paramTypes1);
        Method method2 = ReflectUtil.findMethod(className2, funcName2, paramTypes2);
        boolean success = MethodHook.hookMethod1(method1, method2);
        Log.i(TAG, "hookMethod1 success=" + success);
        if (success) {
            return;
        }

        String decriptor1 = Type.getMethodDescriptor(returnType1, paramTypes1);
        String decriptor2 = Type.getMethodDescriptor(returnType2, paramTypes2);
        success = MethodHook.hookMethod2(className1, funcName1, decriptor1, isStatic1,
                                         className2, funcName2, decriptor2, isStatic2);
        Log.i(TAG, "hookMethod2 success=" + success);
    }

    /**
     * delete optimize file of patch file
     * @param file patch file
     */
    public synchronized void removeOptFile(File file) {
        File optfile = new File(mOptDir, file.getName());
        if (optfile.exists() && !optfile.delete()) {
            Log.e(TAG, optfile.getName() + " delete error.");
        }
    }
}

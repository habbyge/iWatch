package com.habbyge.iwatch;

import android.content.Context;
import android.util.Log;

import com.habbyge.iwatch.loader.SecurityChecker;
import com.habbyge.iwatch.patch.FixMethodAnno;
import com.habbyge.iwatch.util.ReflectUtil;
import com.habbyge.iwatch.util.StringUtil;
import com.habbyge.iwatch.util.Type;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import dalvik.system.DexFile;

/**
 * Created by habbyge on 2021/1/5.
 */
public final class IWatch {
    private static final String TAG = "iWatch.IWatch";

    private static final String DIR = "apatch_opt";

    /**
     * context
     */
    private final Context mContext;

    /**
     * classes will be fixed
     * <className@classloader, fixClass>
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
        Log.i(TAG, "mOptDir=" + mOptDir.getAbsolutePath());
    }

    public void init() {
        MethodHook.init();
    }

    /**
     * fix all class of this patch.apk.
     * @param patchPath patch path
     */
    public synchronized void fix(String patchPath) {
        doFix(new File(patchPath), mContext.getClassLoader(), null);
    }

    /**
     * fix
     *
     * @param patchFile patch file
     * @param cl classloader of class for load patch class
     * @param classNames class names of patch.apk
     */
    public synchronized void fix(File patchFile, ClassLoader cl, List<String> classNames) {
        doFix(patchFile, cl, classNames);
    }

    /**
     * 修复函数的主要任务：
     * 1. 校验补丁包：比对补丁包的签名和应用的签名是否一致
     * 2. 使用 DexFile 和 自定义ClassLoader 来加载补丁包中class文件，与 DexClassLoader加载原理类似，而且
     *    DexClassLoader 内部的加载逻辑也是使用了DexFile来进行操作的，而这里为什么要进行加载操作呢？因为我们
     *    需要获取补丁类中需要修复的方法名称，而这个方法名称是通过修复方法的注解来获取到的，所以我们得先进行类的
     *    加载然后获取到他的方法信息，最后通过分析注解获取方法名，这里用的是反射机制来进行操作的。
     * 3.
     */
    private void doFix(File patchFile, ClassLoader cl, List<String> classNames) {
        if (patchFile == null || !patchFile.exists()) {
            Log.e(TAG, "doFix, patchFile NOT exists");
            return;
        }

        if (!mSecurityChecker.verifyApk(patchFile)) { // 检查patch包完整性、签名
            Log.e(TAG, "doFix, mSecurityChecker.verifyApk failure: " + patchFile.getAbsolutePath());
            return;
        }

        try {
            File optfile = new File(mOptDir, patchFile.getName());
            boolean saveFingerprint = true;
            if (optfile.exists()) {
                // need to verify fingerprint when the optimize file exist,
                // prevent someone attack on jailbreak device with
                // Vulnerability-Parasyte.
                // btw:exaggerated android Vulnerability-Parasyte
                // http://secauo.com/Exaggerated-Android-Vulnerability-Parasyte.html

                if (mSecurityChecker.verifyOpt(optfile)) {
                    Log.i(TAG, "doFix, mSecurityChecker.verifyOpt=" + optfile.getAbsolutePath());
                    saveFingerprint = false;
                } else if (!optfile.delete()) {
                    return;
                }
            }
            Log.i(TAG, "doFix, patchFile, optFile: " + patchFile.getAbsolutePath() + ", "
                                                     + optfile.getAbsolutePath());

            // libcore/dalvik/src/main/java/dalvik/system/DexFile.java
            // 使用 DexFile 来加载 patch 包文件，所以补丁包其实是一个包装了的dex文件
            final DexFile dexFile = DexFile.loadDex(patchFile.getAbsolutePath(),
                                                    optfile.getAbsolutePath(),
                                                    Context.MODE_PRIVATE);

            if (saveFingerprint) {
                mSecurityChecker.saveOptSig(optfile);
            }

            // 双亲机制，这里也是关键点之1/2，classLoader 决定的是该 补丁.apk 中被加载到内存中的class，
            // 是否能够被原apk识别，技术原理与 DexClassLoader 相似.
            ClassLoader patchClassLoader = new ClassLoader(cl) {

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
                    fixClass(clazz, cl);
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "pacth", e);
        }
    }

    /**
     * fix class
     * @param clazz patch.apk 中的 class
     */
    private void fixClass(Class<?> clazz, ClassLoader cl) {
        Method[] methods = clazz.getDeclaredMethods();
        FixMethodAnno fixMethodAnno;
        String originClassName;
        String originMethodName;
        boolean originStatic;
        for (Method method : methods) {
            fixMethodAnno = method.getAnnotation(FixMethodAnno.class);
            if (fixMethodAnno == null) {
                continue;
            }
            originClassName = fixMethodAnno._class();
            originMethodName = fixMethodAnno.method();
            originStatic = Modifier.isStatic(method.getModifiers());
            if (StringUtil.isNotEmpty(originClassName) && StringUtil.isNotEmpty(originMethodName)) {
                fixMethod(cl,
                        originClassName, originMethodName, method.getParameterTypes(),
                        method.getReturnType(), originStatic,
                        clazz.getCanonicalName(), method.getName(), method.getParameterTypes(),
                        method.getReturnType(), originStatic);
            }
        }
    }

    /**
     * fix method, 2 -> 1
     */
    private void fixMethod(ClassLoader cl, String className1, String funcName1,
                           Class<?>[] paramTypes1, Class<?> returnType1, boolean isStatic1,
                           String className2, String funcName2, Class<?>[] paramTypes2,
                           Class<?> returnType2, boolean isStatic2) {

        try {
            String key = className1 + "@" + cl.toString();
            Class<?> class1 = mFixedClass.get(key);
            if (class1 == null) { // class not load
                Class<?> _class1 = cl.loadClass(className1);
                // initialize target class
                class1 = Class.forName(_class1.getName(), true, _class1.getClassLoader());
            }
            mFixedClass.put(key, class1);

            hook(className1, funcName1, paramTypes1, returnType1, isStatic1,
                 className2, funcName2, paramTypes2, returnType2, isStatic2);
        } catch (Exception e) {
            Log.e(TAG, "fixMethod", e);
        }
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
        boolean success = MethodHook.hookMethod1(className1, funcName1, funcName2, method1, method2);
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

    public void unhookAllMethod() {
        MethodHook.unhookAllMethod();
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

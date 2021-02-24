package com.habbyge.iwatch;

import android.content.Context;
import android.util.Log;

import com.habbyge.iwatch.loader.SecurityChecker;
import com.habbyge.iwatch.patch.FixMethodAnno;
import com.habbyge.iwatch.util.FileUtil;
import com.habbyge.iwatch.util.ReflectUtil;
import com.habbyge.iwatch.util.StringUtil;
import com.habbyge.iwatch.util.Type;

import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;

import dalvik.system.DexClassLoader;

/**
 * Created by habbyge on 2021/1/5.
 */
public final class IWatch {
    private static final String TAG = "iWatch.IWatch";

    private static final String DIR = "apatch_opt";

    private final SecurityChecker mSecurityChecker;

    private final File mOptDir; // optimize directory

    public IWatch(Context context) {
        mSecurityChecker = new SecurityChecker(context);
        mOptDir = new File(context.getFilesDir(), DIR);
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
     *
     * @param patchPath patch path
     */
    @SuppressWarnings("unused")
    public synchronized void fix(String patchPath) {
        doFix(new File(patchPath), null);
    }

    /**
     * fix
     *
     * @param patchFile  patch file
     * @param classNames class names of patch.apk
     */
    public synchronized void fix(File patchFile, List<String> classNames) {
        doFix(patchFile, classNames);
    }

    /**
     * 修复函数的主要任务：
     * 1. 校验补丁包：比对补丁包的签名和应用的签名是否一致
     * 2. 使用 DexFile 和 自定义ClassLoader 来加载补丁包中class文件，与 DexClassLoader加载原理类似，而且
     * DexClassLoader 内部的加载逻辑也是使用了DexFile来进行操作的，而这里为什么要进行加载操作呢？因为我们
     * 需要获取补丁类中需要修复的方法名称，而这个方法名称是通过修复方法的注解来获取到的，所以我们得先进行类的
     * 加载然后获取到他的方法信息，最后通过分析注解获取方法名，这里用的是反射机制来进行操作的。
     * 3.
     *
     * @param classNames patchFile 中需要被 patch 的 类
     */
    private void doFix(File patchFile, List<String> classNames) {
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
            if (saveFingerprint) {
                mSecurityChecker.saveOptSig(optfile);
            }

            String patchFilePath = patchFile.getAbsolutePath();
            Log.i(TAG, "doFix, patchFile, optFile: " + patchFilePath + ", " + optfile.getAbsolutePath());
            final ClassLoader cl = IWatch.class.getClassLoader();
            final DexClassLoader dexCl = new DexClassLoader(patchFilePath, optfile.getAbsolutePath(), null, cl);
            Enumeration<JarEntry> jarEntries = FileUtil.parseJarFile(patchFile);
            if (jarEntries == null) {
                Log.e(TAG, "doFix, jarEntries is NULL");
                return;
            }
            Class<?> clazz;
            String entry;
            while (jarEntries.hasMoreElements()) {
                entry = jarEntries.nextElement().getName();
                if (classNames != null && !classNames.contains(entry)) {
                    continue; // skip, not need fix
                }
                clazz = dexCl.loadClass(entry);
                // 这里之后，patch中的类在其自定义的ClassLoader中已经加载完毕了，即在虚拟机(Art)中的地址已经确定了,
                // 这样就可可以直接执行后续的地址替换了，跟 ClassLoader 无关了.
                if (clazz != null) {
                    fixClass(cl, clazz);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "pacth", e);
        }
    }

    private void fixClass(ClassLoader cl, Class<?> clazz) {
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
                // 方案1:
                if (fixMethod1(cl, originClassName, originMethodName, method)) {
                    Log.i(TAG, "fixMethod1 success !");
                    continue;
                }

                // TODO: 2021/2/23 继续 方案2:
                if (fixMethod2(
                        originClassName, originMethodName, method.getParameterTypes(),
                        method.getReturnType(), originStatic,
                        clazz.getCanonicalName(), method.getName(), method.getParameterTypes(),
                        method.getReturnType(), originStatic)) {

                    Log.i(TAG, "fixMethod2 success !");
                }
            }
        }
    }

    /**
     * @param cl 宿主 ClassLoader
     * @return 是否 fix 成功
     */
    public boolean fixMethod1(ClassLoader cl, String className1, String funcName1, Method method2) {
        Class<?>[] paramTypes = method2.getParameterTypes();
        String desc = Type.getMethodDescriptor(method2.getReturnType(), paramTypes);
        Method method1 = ReflectUtil.findMethod(cl, className1, funcName1, paramTypes);
        return MethodHook.hookMethod1(className1, funcName1, desc, method1, method2);
    }

    private boolean fixMethod2(String className1, String funcName1,
                               Class<?>[] paramTypes1, Class<?> returnType1, boolean isStatic1,
                               String className2, String funcName2, Class<?>[] paramTypes2,
                               Class<?> returnType2, boolean isStatic2) {
// TODO: 2021/2/23 ing......
        if (StringUtil.isEmpty(className1) || StringUtil.isEmpty(funcName1)
                || StringUtil.isEmpty(className2) || StringUtil.isEmpty(funcName2)
                || returnType1 == null || returnType2 == null) {

            throw new NullPointerException("IWatch.hook, param is null");
        }

        String decriptor1 = Type.getMethodDescriptor(returnType1, paramTypes1);
        String decriptor2 = Type.getMethodDescriptor(returnType2, paramTypes2);
        return MethodHook.hookMethod2(className1, funcName1, decriptor1, isStatic1,
                                      className2, funcName2, decriptor2, isStatic2);
    }

    public void unhookAllMethod() {
        MethodHook.unhookAllMethod();
    }

    /**
     * delete optimize file of patch file
     *
     * @param file patch file
     */
    public synchronized void removeOptFile(File file) {
        File optfile = new File(mOptDir, file.getName());
        if (optfile.exists() && !optfile.delete()) {
            Log.e(TAG, optfile.getName() + " delete error.");
        }
    }
}

package com.habbyge.iwatch;

import android.text.TextUtils;
import android.util.Log;

import com.habbyge.iwatch.patch.FixMethodAnno;
import com.habbyge.iwatch.util.FileUtil;
import com.habbyge.iwatch.util.ReflectUtil;
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

//    private final SecurityChecker mSecurityChecker;

    public IWatch() {
//        mSecurityChecker = new SecurityChecker(context);
    }

    public void init() {
        MethodHook.init();
    }

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

        /*if (!mSecurityChecker.verifyApk(patchFile)) { // 检查patch包完整性、签名
            Log.e(TAG, "doFix, mSecurityChecker.verifyApk failure: " + patchFile.getAbsolutePath());
            return;
        }*/

        try {
            String patchFilePath = patchFile.getAbsolutePath();
            Log.i(TAG, "doFix, patchFile: " + patchFilePath);
            final ClassLoader cl = IWatch.class.getClassLoader();
            final DexClassLoader dexCl = new DexClassLoader(patchFilePath, null, null, cl);
            Enumeration<JarEntry> jarEntries = FileUtil.parseJarFile(patchFile);
            if (jarEntries == null) {
                Log.e(TAG, "doFix, jarEntries is NULL");
                return;
            }
            Class<?> clazz;
            for (String className : classNames) {
                clazz = dexCl.loadClass(className);
                if (clazz != null) {
                    fixClass(cl, clazz);
                }
            }

//            Class<?> clazz
//            String entry;
//            while (jarEntries.hasMoreElements()) { // 这里应该是.dex文件
//                entry = jarEntries.nextElement().getName();
//                if (classNames != null && !classNames.contains(entry)) {
//                    continue; // skip, not need fix
//                }
//                clazz = dexCl.loadClass(entry);
//                // 这里之后，patch中的类在其自定义的ClassLoader中已经加载完毕了，即在虚拟机(Art)中的地址已经确定了,
//                // 这样就可可以直接执行后续的地址替换了，跟 ClassLoader 无关了.
//                if (clazz != null) {
//                    fixClass(cl, clazz);
//                }
//            }
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
            if (!TextUtils.isEmpty(originClassName) && !TextUtils.isEmpty(originMethodName)) {
                // 方案1:
                if (fixMethod1(cl, originClassName, originMethodName, method)) {
                    Log.i(TAG, "fixMethod1 success !");
                    continue;
                }
                // 方案2:
                // 通过阅读 DexClassLoader.loadClass()源码
                // (libcore/dalvik/src/main/java/dalvik/system/DexClassLoader.java)，可知：
                // 一个class在虚拟机(Art)中的标识是其: 全路径名@classLoader，那么我们通过自定义DexClassLoader
                // 加载的patch中的class，在Art虚拟机中已经是我们自定义的classLoader了，因此这里是可用于处理补丁包
                // 中的类的。
                // 其中使用到 ClassLoader 的地方是：DexPathList.findClass()，初始化其ClassLoader是在DexPathList
                // 构造函数中，直接在 DexClassLoader 中赋值this，也就是我们自定义的 ClassLoader.
                // 上面已经loadCLass过补丁中的class，那么第2次使用时，直接从缓存中读取即可。
                // DexFile在Art中的实现对应：art/runtime/native/dalvik_system_DexFile.cc 中的 DexFile_defineClassNative函数
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
    private boolean fixMethod1(ClassLoader cl, String className1, String funcName1, Method method2) {
        Class<?>[] paramTypes = method2.getParameterTypes();
        String desc = Type.getMethodDescriptor(method2.getReturnType(), paramTypes);
        Method method1 = ReflectUtil.findMethod(cl, className1, funcName1, paramTypes);
        method2.setAccessible(true);
        return MethodHook.hookMethod1(className1, funcName1, desc, method1, method2);
    }

    private boolean fixMethod2(String className1, String funcName1,
                               Class<?>[] paramTypes1, Class<?> returnType1, boolean isStatic1,
                               String className2, String funcName2, Class<?>[] paramTypes2,
                               Class<?> returnType2, boolean isStatic2) {

        if (TextUtils.isEmpty(className1) || TextUtils.isEmpty(funcName1)
                || TextUtils.isEmpty(className2) || TextUtils.isEmpty(funcName2)
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
}

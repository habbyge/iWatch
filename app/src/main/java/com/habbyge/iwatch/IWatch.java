package com.habbyge.iwatch;

import android.text.TextUtils;
import android.util.Log;

import com.alipay.euler.andfix.annotation.MethodReplace;
import com.habbyge.iwatch.patch.FixMethodAnno;
import com.habbyge.iwatch.util.FileUtil;
import com.habbyge.iwatch.util.ReflectUtil;
import com.habbyge.iwatch.util.Type;

import java.io.File;
import java.lang.reflect.Field;
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

    private final boolean mTest;

    public IWatch(boolean test) {
        mTest = test;
    }

    public void init() {
        MethodHook.init();
    }

    private Class<?> mMethodReplaceClass = null;
    private Class<?> mFixMethodAnnoClass = null;

    /**
     * 修复函数的主要任务：
     * 1. 校验补丁包：比对补丁包的签名和应用的签名是否一致
     * 2. 使用 DexFile 和 自定义ClassLoader 来加载补丁包中class文件，与 DexClassLoader加载原理类似，而且
     * DexClassLoader 内部的加载逻辑也是使用了DexFile来进行操作的，而这里为什么要进行加载操作呢？因为我们
     * 需要获取补丁类中需要修复的方法名称，而这个方法名称是通过修复方法的注解来获取到的，所以我们得先进行类的
     * 加载然后获取到他的方法信息，最后通过分析注解获取方法名，这里用的是反射机制来进行操作的。
     *
     * @param classNames patchFile 中需要被 patch 的 类
     */
    public synchronized void fix(File patchFile, List<String> classNames) {
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
            DexClassLoader dexCl = new DexClassLoader(patchFilePath, null, null, cl);
            Enumeration<JarEntry> jarEntries = FileUtil.parseJarFile(patchFile);
            if (jarEntries == null) {
                Log.e(TAG, "doFix, jarEntries is NULL");
                return;
            }
            // TODO: 2021/3/2 这里利用 jarEntries 字段来查看 patch 中所有的 class 文件？？？？？？
            JarEntry entry;
            while (jarEntries.hasMoreElements()) {
                entry = jarEntries.nextElement();
                Log.d(TAG, "fix: patch entry=" + entry.getName());
            }
            // TODO: 2021/3/2 这里利用 jarEntries 字段来查看 patch 中所有的 class 文件？？？？？？

            // 加载FixMethodAnno/MethodReplace的ClassLoader必须是补丁的DexClassLoader，如果直接写 FixMethodAnno.class，
            // 则是来自于宿主app.apk的ClassLoader，在patch中是识别不到的，因为写在补丁中的注解(Annotation)是来自于补丁，传递
            // 宿主的注解class，显然在虚拟机中的地址是不正确的，所以胡返回null。
            if (mTest) {
                if (mMethodReplaceClass == null) {
                    mMethodReplaceClass = Class.forName("com.alipay.euler.andfix.annotation.MethodReplace", true, dexCl);
                }
            } else {
                if  (mFixMethodAnnoClass == null) {
                    mFixMethodAnnoClass = Class.forName("com.habbyge.iwatch.patch.FixMethodAnno", true, dexCl);
                }
            }

            Class<?> clazz;
            for (String className : classNames) {
                clazz = dexCl.loadClass(className);
                if (clazz != null) {
                    fixClass(cl, clazz);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "pacth", e);
        }
    }

    private void fixClass(ClassLoader cl, Class<?> clazz) {
        if (mTest) {
            doFixClassTest(cl, clazz);
        } else {
            doFixClass(cl, clazz);
        }
    }

    private void doFixClass(ClassLoader cl, Class<?> clazz) {
        setAccessPublic(clazz);

        Method[] methods = clazz.getDeclaredMethods();
        FixMethodAnno fixMethodAnno;
        String originClassName;
        String originMethodName;
        boolean originStatic;
        for (Method method : methods) {
            /*fixMethodAnno = method.getAnnotation(FixMethodAnno.class);*/
            // noinspection unchecked
            fixMethodAnno = method.getAnnotation((Class<FixMethodAnno>) mFixMethodAnnoClass);
            Log.w(TAG, "doFixClass, clazz=" + clazz.getName() +
                    ", method=" + method.getName() +
                    ", annotation=" + (fixMethodAnno == null ? "nullptr" : "not nullptr"));
            if (fixMethodAnno == null) {
                continue;
            }
            originClassName = fixMethodAnno._class();
            originMethodName = fixMethodAnno.method();
            originStatic = Modifier.isStatic(method.getModifiers());
            if (!TextUtils.isEmpty(originClassName) && !TextUtils.isEmpty(originMethodName)) {
                setAccessPublic(cl, originClassName); // 这里需要让原始class中的所有字段和方法为public

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

    private void doFixClassTest(ClassLoader cl, Class<?> clazz) {
        setAccessPublic(clazz);
        Method[] methods = clazz.getDeclaredMethods();

        MethodReplace methodReplace;
        String originClassName;
        String originMethodName;
        boolean originStatic;
        for (Method method : methods) {
            method.setAccessible(true);

            // 这里会拿到为null，因为这里需要patch中的DexClassLoader
            /*methodReplace = method.getAnnotation(MethodReplace.class);*/
            // noinspection unchecked
            methodReplace = method.getAnnotation((Class<MethodReplace>) mMethodReplaceClass);
            if (methodReplace == null) {
                methodReplace = method.getAnnotation(MethodReplace.class);
            }
            Log.w(TAG, "doFixClassTest, clazz=" + clazz.getName() +
                    ", method=" + method.getName() +
                    ", annotation=" + (methodReplace == null ? "nullptr" : "not nullptr"));
            if (methodReplace == null) {
                continue;
            }

            originClassName = methodReplace.clazz();
            originMethodName = methodReplace.method();
            originStatic = Modifier.isStatic(method.getModifiers());
            if (!TextUtils.isEmpty(originClassName) && !TextUtils.isEmpty(originMethodName)) {
                setAccessPublic(cl, originClassName); // 这里需要让原始class中的所有字段和方法为public

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

    private void setAccessPublic(ClassLoader cl, String className) {
        Class<?> Class1;
        try {
            Class1 = cl.loadClass(className);
        } catch (ClassNotFoundException e) {
            Log.e(TAG, "setAccessPublic, exception: " + e.getMessage());
            return;
        }

        Field[] fields1 = Class1.getDeclaredFields();
        if (fields1.length > 0) {
            for (Field field : fields1) {
                MethodHook.setFieldAccessPublic(field);
            }
        }

        Method[] methods = Class1.getDeclaredMethods();
        if (methods.length > 0) {
            for (Method method : methods) {
                MethodHook.setMethodAccessPublic(method);
            }
        }
    }

    private void setAccessPublic(Class<?> clazz) {
        Field[] fields2 = clazz.getDeclaredFields();
        if (fields2.length > 0) {
            for (Field field : fields2) {
                MethodHook.setFieldAccessPublic(field);
            }
        }

        Method[] methods = clazz.getDeclaredMethods();
        if (methods.length > 0) {
            for (Method method : methods) {
                method.setAccessible(true);
                MethodHook.setMethodAccessPublic(method);
            }
        }
    }
}

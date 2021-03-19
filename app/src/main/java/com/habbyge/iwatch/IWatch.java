package com.habbyge.iwatch;

import android.text.TextUtils;
import android.util.Log;

import com.habbyge.iwatch.patch.FixMethodAnno;
import com.habbyge.iwatch.util.ReflectUtil;
import com.habbyge.iwatch.util.Type;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;

import dalvik.system.DexClassLoader;

/**
 * Created by habbyge on 2021/1/5.
 */
public final class IWatch {
    private static final String TAG = "iWatch.IWatch";

    public IWatch() {
        MethodHook.init();
    }

    public synchronized void fix(File patchFile, List<String> classNames) {
        if (patchFile == null || !patchFile.exists() || classNames == null || classNames.isEmpty()) {
            Log.e(TAG, "doFix, patchFile NOT exists");
            return;
        }

        // 这个DexFile已经被标记 @Deprecated 了，也就是将来不对外使用了，这里采用了替代方案：
        // dalvik/system/DexFile中的nativev方法对应Art中的C++文件是:
        // art/runtime/native/dalvik_system_DexFile.cc，例如: 打开DexFile是:
        // openDexFileNative -> DexFile_openDexFileNative
        ClassLoader cl = IWatch.class.getClassLoader();
        if (cl == null) {
            return;
        }
        DexClassLoader dexCl = new DexClassLoader(patchFile.getAbsolutePath(), null, null, cl);
        Log.w(TAG, "classLoader=" + cl.getClass().getName() + ", " + dexCl.getClass().getName());
        try {
            for (String className : classNames) {
                fixClass(dexCl.loadClass(className), cl);
            }
        } catch (ClassNotFoundException e) {
            Log.e(TAG, "iwatch fix: e=" + e.getMessage());
        }
    }

    private void fixClass(Class<?> clazz, ClassLoader cl) {
        Method[] methods = clazz.getDeclaredMethods();
        Log.d(TAG, "fixClass, methods=" + methods.length);

        FixMethodAnno anno;
        String className1;
        String methodName1;
        boolean static1;

        for (Method method : methods) {
            // 这里会拿到为null，因为这里需要patch中的DexClassLoader
            anno = method.getAnnotation(FixMethodAnno.class);
            if (anno == null) { // _CF class 中的其他非 FixMethodAnno method 也需要 replace
                continue;
            }

            className1 = anno.clazz();
            methodName1 = anno.method();
            if (!TextUtils.isEmpty(className1) && !TextUtils.isEmpty(methodName1)) {
                setAccessPublic(cl, className1); // 这里需要让原始class中的所有字段和方法为public
                /*setAccessPublic(pcl, className1); // 让补丁能使用补丁中新增的类*/

                // 方案1:
                if (fixMethod1(cl, className1, methodName1, method)) {
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
                // DexFile在Art中的实现对应：art/runtime/native/dalvik_system_DexFile.cc 中
                // 的 DexFile_defineClassNative函数
                static1 = Modifier.isStatic(method.getModifiers());
                if (fixMethod2(
                        className1, methodName1, method.getParameterTypes(),
                        method.getReturnType(), static1,
                        clazz.getCanonicalName(), method.getName(), method.getParameterTypes(),
                        method.getReturnType(), static1)) {

                    Log.i(TAG, "fixMethod2 success !");
                }
            }
        }
    }

    public static boolean fixMethod1(ClassLoader cl, String className1, String funcName1, Method method2) {
        Class<?>[] paramTypes = method2.getParameterTypes();
        String desc = Type.getMethodDescriptor(method2.getReturnType(), paramTypes);
        Method method1 = ReflectUtil.findMethod(cl, className1, funcName1, paramTypes);
        method2.setAccessible(true);
        Log.d(TAG, "fixMethod1, oldClassName: " + className1 + ", " + funcName1);

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
        Class<?> clazz;
        try {
            clazz = cl.loadClass(className);
        } catch (ClassNotFoundException e) {
            Log.e(TAG, "setAccessPublic, exception: " + e.getMessage());
            return;
        }
        setAccessPublic(clazz);
    }

    private void setAccessPublic(Class<?> clazz) {
        Field[] fields = clazz.getDeclaredFields();
        if (fields.length > 0) {
            String desc;
            boolean isStatic;
            for (Field field : fields) {
                Log.d(TAG, "set public, field: " + clazz.getName() + ", field=" + field.getName());
                desc = Type.getDescriptor(field.getType());
                isStatic = Modifier.isStatic(field.getModifiers());
                MethodHook.setFieldAccessPublic(field, clazz, field.getName(), desc, isStatic);
            }
        }

        Method[] methods = clazz.getDeclaredMethods();
        if (methods.length > 0) {
            for (Method method : methods) {
                Log.d(TAG, "set public, method: " + clazz.getName() + ", method=" + method.getName());
                MethodHook.setMethodAccessPublic(method);
            }
        }
    }
}

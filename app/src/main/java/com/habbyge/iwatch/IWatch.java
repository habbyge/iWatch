package com.habbyge.iwatch;

import android.text.TextUtils;
import android.util.Log;

import com.habbyge.iwatch.patch.FixMethodAnno;
import com.habbyge.iwatch.util.Type;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import dalvik.system.DexClassLoader;

public final class IWatch {
    private static final String TAG = "iWatch.IWatch";
    private final List<String> mAccClassList;

    public IWatch() {
        MethodHook.init();
        mAccClassList = new ArrayList<>();
    }

    public synchronized boolean fix(File patchFile, List<String> classNames) {
        // 这个DexFile已经被标记 @Deprecated 了，也就是将来不对外使用了，这里采用了替代方案：
        // dalvik/system/DexFile中的nativev方法对应Art中的C++文件是:
        // art/runtime/native/dalvik_system_DexFile.cc，例如: 打开DexFile是:
        // openDexFileNative -> DexFile_openDexFileNative
        ClassLoader cl = IWatch.class.getClassLoader();
        if (cl == null) {
            Log.e(TAG, "cl is null !");
            return false;
        }
        DexClassLoader dexCl = new DexClassLoader(patchFile.getAbsolutePath(), null, null, cl);
        try {
            for (String className : classNames) {
                fixClass(dexCl.loadClass(className), cl);
            }
            return true;
        } catch (ClassNotFoundException e) {
            Log.e(TAG, "iWatch fix: e=" + e.getMessage());
            return false;
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
                if (fixMethod1(cl, className1, methodName1, method)) { // 方案1:
                    Log.i(TAG, "fixMethod1 success !");
                    continue;
                }

                // 方案3:
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
                if (fixMethod2(cl, method,
                        className1, methodName1, method.getParameterTypes(),
                        method.getReturnType(), static1,
                        clazz.getCanonicalName(),
                        method.getName(), method.getParameterTypes(),
                        method.getReturnType(), static1)) {

                    Log.i(TAG, "fixMethod2 success !");
                }
            }
        }
    }

    public boolean fixMethod1(ClassLoader cl, String className1, String funcName1, Method method2) {
        Class<?>[] paramTypes = method2.getParameterTypes();
        String desc = Type.getMethodDesc(method2.getReturnType(), paramTypes);
        Method method1;
        try {
            Class<?> clazz1 = cl.loadClass(className1);
            method1 = clazz1.getDeclaredMethod(funcName1, paramTypes);
            method1.setAccessible(true);

            if (!mAccClassList.contains(className1)) {
                setAccessPublic(clazz1);
                mAccClassList.add(className1);
            }
        } catch (Exception e) {
            Log.e(TAG, "fixMethod1 loadClass crash: " + e.getMessage());
            return false;
        }

        method2.setAccessible(true);
        Log.d(TAG, "fixMethod1, oldClassName: " + className1 + ", " + funcName1);

        return MethodHook.hookMethod1(className1, funcName1, desc, method1, method2);
    }

    private boolean fixMethod2(ClassLoader cl, Method method2,
                               String className1, String funcName1, Class<?>[] paramTypes1,
                               Class<?> returnType1, boolean isStatic1, String className2,
                               String funcName2, Class<?>[] paramTypes2,
                               Class<?> returnType2, boolean isStatic2) {

        if (TextUtils.isEmpty(className1) || TextUtils.isEmpty(funcName1)
                || TextUtils.isEmpty(className2) || TextUtils.isEmpty(funcName2)
                || returnType1 == null || returnType2 == null) {

            throw new NullPointerException("iWatch.hook, param is null");
        }

        Method method1;
        try {
            Class<?> clazz1 = cl.loadClass(className1);
            method1 = clazz1.getDeclaredMethod(funcName1, paramTypes1);
            method1.setAccessible(true);

            if (!mAccClassList.contains(className1)) {
                setAccessPublic(clazz1);
                mAccClassList.add(className1);
            }
        } catch (Exception e) {
            Log.e(TAG, "fixMethod2 loadClass crash: " + e.getMessage());
            return false;
        }

        String decriptor1 = Type.getMethodDesc(returnType1, paramTypes1);
        String decriptor2 = Type.getMethodDesc(returnType2, paramTypes2);
        return MethodHook.hookMethod2(method1, method2,
                className1, funcName1, decriptor1, isStatic1,
                className2, funcName2, decriptor2, isStatic2);
    }

    public void reset() {
        MethodHook.unhookAllMethod();
        mAccClassList.clear();
    }

    private void setAccessPublic(Class<?> clazz) {
        Field[] fields = clazz.getDeclaredFields();
        if (fields.length > 0) {
            String name;
            String desc;
            boolean isStatic;
            for (Field field : fields) {
                name = field.getName();
                desc = Type.getDesc(field.getType());
                isStatic = Modifier.isStatic(field.getModifiers());
                MethodHook.setFieldAccessPublic(field, clazz, name, desc, isStatic);
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

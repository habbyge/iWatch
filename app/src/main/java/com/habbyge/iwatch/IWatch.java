package com.habbyge.iwatch;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.alipay.euler.andfix.annotation.MethodReplace;
import com.habbyge.iwatch.patch.FixMethodAnno;
import com.habbyge.iwatch.patch.Patch;
import com.habbyge.iwatch.util.ReflectUtil;
import com.habbyge.iwatch.util.Type;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Enumeration;
import java.util.List;

import dalvik.system.DexFile;

/**
 * Created by habbyge on 2021/1/5.
 */
public final class IWatch {
    private static final String TAG = "iWatch.IWatch";

    private final boolean mTest;
    private final File mOptDir;   // optimize directory

    public IWatch(Context context, boolean test) {
        mTest = test;

        mOptDir = new File(context.getFilesDir(), Patch.DIR);
        if (!mOptDir.exists() && !mOptDir.mkdirs()) {// make directory fail
            Log.e(TAG, "opt dir create error.");
        } else if (!mOptDir.isDirectory()) {// not directory
            boolean ret = mOptDir.delete();
            Log.i(TAG, "mOptDir.delete(): " + ret);
        }
    }

    public void init() {
        MethodHook.init();
    }

//    private Class<?> mMethodReplaceClass = null;
//    private Class<?> mFixMethodAnnoClass = null;

    public synchronized void fix(File patchFile, List<String> classNames) {
        if (patchFile == null || !patchFile.exists()) {
            Log.e(TAG, "doFix, patchFile NOT exists");
            return;
        }

        try {
            File optfile = new File(mOptDir, patchFile.getName());
            if (optfile.exists()) {
                if (!optfile.delete()) {
                    return;
                }
            }

            final DexFile dexFile = DexFile.loadDex(patchFile.getAbsolutePath(),
                                                    optfile.getAbsolutePath(),
                                                    Context.MODE_PRIVATE);

            final ClassLoader classLoader = IWatch.class.getClassLoader();
            ClassLoader patchClassLoader = new ClassLoader(classLoader) {

                @Override
                protected Class<?> findClass(String className) throws ClassNotFoundException {
                    String packagePath1 = "com.alipay.euler.andfix";
                    String packagePath2 = "com.habbyge.iwatch";

                    Class<?> clazz = dexFile.loadClass(className, this);
                    if (clazz == null /*&& (className.startsWith(packagePath1)
                            || className.startsWith(packagePath2))*/) {
                        return Class.forName(className);// annotation’s class
                    }
//                    if (clazz == null) {
//                        throw new ClassNotFoundException(className);
//                    }
                    return clazz;
                }
            };


            Enumeration<String> entrys = dexFile.entries();
            Class<?> clazz;
            while (entrys.hasMoreElements()) { // 遍历补丁中的class文件
                String entry = entrys.nextElement();
                clazz = dexFile.loadClass(entry, patchClassLoader);
                if (clazz == null) {
                    Log.e(TAG, "fix: loadClass failure: " + entry);
                    return;
                }
                setAccessPublic(clazz);

                if (classNames != null && !classNames.contains(entry)) {
                    continue;// skip, not need fix
                }
                fixClass(classLoader, clazz);
            }
        } catch (Exception e) {
            Log.e(TAG, "pacth", e);
        }
    }

//    /**
//     * 修复函数的主要任务：
//     * 1. 校验补丁包：比对补丁包的签名和应用的签名是否一致
//     * 2. 使用 DexFile 和 自定义ClassLoader 来加载补丁包中class文件，与 DexClassLoader加载原理类似，而且
//     * DexClassLoader 内部的加载逻辑也是使用了DexFile来进行操作的，而这里为什么要进行加载操作呢？因为我们
//     * 需要获取补丁类中需要修复的方法名称，而这个方法名称是通过修复方法的注解来获取到的，所以我们得先进行类的
//     * 加载然后获取到他的方法信息，最后通过分析注解获取方法名，这里用的是反射机制来进行操作的。
//     *
//     * @param classNames patchFile 中需要被 patch 的 类
//     */
//    public synchronized void fix(File patchFile, List<String> classNames) {
//        if (patchFile == null || !patchFile.exists()) {
//            Log.e(TAG, "doFix, patchFile NOT exists");
//            return;
//        }
//
//        try {
//            String patchFilePath = patchFile.getAbsolutePath();
//            Log.i(TAG, "doFix, patchFile: " + patchFilePath);
//            final ClassLoader cl = IWatch.class.getClassLoader();
//
//            // TODO: 2021/3/3 这里改为 DexFile 加载 ？？？？？？
//            DexClassLoader dexCl = new DexClassLoader(patchFilePath, null, null, cl);
//
//            Enumeration<JarEntry> jarEntries = FileUtil.parseJarFile(patchFile);
//            if (jarEntries == null) {
//                Log.e(TAG, "doFix, jarEntries is NULL");
//                return;
//            }
//
//            // 加载FixMethodAnno/MethodReplace的ClassLoader必须是补丁的DexClassLoader，如果直接写 FixMethodAnno.class，
//            // 则是来自于宿主app.apk的ClassLoader，在patch中是识别不到的，因为写在补丁中的注解(Annotation)是来自于补丁，传递
//            // 宿主的注解class，显然在虚拟机中的地址是不正确的，所以胡返回null。
//            if (mTest) {
//                if (mMethodReplaceClass == null) {
//                    mMethodReplaceClass = Class.forName("com.alipay.euler.andfix.annotation.MethodReplace", true, dexCl);
//                }
//            } else {
//                if  (mFixMethodAnnoClass == null) {
//                    mFixMethodAnnoClass = Class.forName("com.habbyge.iwatch.patch.FixMethodAnno", true, dexCl);
//                }
//            }
//
//            Class<?> clazz;
//            for (String className : classNames) {
//                clazz = dexCl.loadClass(className);
//                if (clazz != null) {
//                    fixClass(cl, dexCl, clazz);
//                }
//            }
//        } catch (Exception e) {
//            Log.e(TAG, "pacth", e);
//        }
//    }

    private void fixClass(ClassLoader cl, Class<?> clazz) {
        if (mTest) {
            doFixClassTest(cl, clazz);
        } else {
            doFixClass(cl, clazz);
        }
    }

    private void doFixClass(ClassLoader cl, Class<?> clazz) {
        Method[] methods = setAccessPublic(clazz);

        FixMethodAnno fixMethodAnno;
        String className1;
        String methodName1;
        boolean static1;
        for (Method method : methods) {
            /*fixMethodAnno = method.getAnnotation(FixMethodAnno.class);*/
            // noinspection unchecked
            fixMethodAnno = method.getAnnotation(FixMethodAnno.class);
//            fixMethodAnno = method.getAnnotation((Class<FixMethodAnno>) mFixMethodAnnoClass);
//            Log.w(TAG, "doFixClass, clazz=" + clazz.getName() +
//                    ", method=" + method.getName() +
//                    ", annotation=" + (fixMethodAnno == null ? "nullptr" : "not nullptr"));
            if (fixMethodAnno == null) {
                continue;
            }
            className1 = fixMethodAnno._class();
            methodName1 = fixMethodAnno.method();
            if (!TextUtils.isEmpty(className1) && !TextUtils.isEmpty(methodName1)) {
                setAccessPublic(cl, className1); // 这里需要让原始class中的所有字段和方法为public

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
                // DexFile在Art中的实现对应：art/runtime/native/dalvik_system_DexFile.cc 中的 DexFile_defineClassNative函数
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

    private void doFixClassTest(ClassLoader classLoader, Class<?> clazz) {
        Method[] methods = clazz.getDeclaredMethods();
        Log.d(TAG, "doFixClassTest, methods=" + methods.length);

        MethodReplace methodReplace;
        String className1;
        String methodName1;
        boolean static1;

        for (Method method : methods) {
            // 这里会拿到为null，因为这里需要patch中的DexClassLoader
            /*methodReplace = method.getAnnotation(MethodReplace.class);*/
            // noinspection unchecked
//            methodReplace = method.getAnnotation((Class<MethodReplace>) mMethodReplaceClass);
//            if (methodReplace == null) {
                methodReplace = method.getAnnotation(MethodReplace.class);
//            }
            Log.w(TAG, "doFixClassTest, clazz=" + clazz.getName() +
                    ", method=" + method.getName() +
                    ", annotation=" + (methodReplace == null ? "nullptr" : "NOT-nullptr"));

            if (methodReplace == null) {
                continue;
            }

            className1 = methodReplace.clazz();
            methodName1 = methodReplace.method();
            if (!TextUtils.isEmpty(className1) && !TextUtils.isEmpty(methodName1)) {
                Log.d(TAG, "doFixClassTest, oldClassName=" + className1); // TODO: 2021/3/3 ing
                setAccessPublic(classLoader, className1); // 这里需要让原始class中的所有字段和方法为public

                // 方案1:
                if (fixMethod1(classLoader, className1, methodName1, method)) {
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

    private void setAccessPublic(ClassLoader classLoader, String className) {
        Class<?> class1;
        try {
            class1 = classLoader.loadClass(className);
        } catch (ClassNotFoundException e) {
            Log.e(TAG, "setAccessPublic, exception: " + e.getMessage());
            return;
        }
        Field[] fields1 = class1.getDeclaredFields();
        if (fields1.length > 0) {
            String desc;
            boolean isStatic;
            for (Field field : fields1) {
                Log.d(TAG, "set public: " + className + ", field=" + field.getName());
                desc = Type.getDescriptor(field.getType());
                isStatic = Modifier.isStatic(field.getModifiers());
                MethodHook.setFieldAccessPublic(field, class1, field.getName(), desc, isStatic);
            }
        }
        Method[] methods = class1.getDeclaredMethods();
        if (methods.length > 0) {
            for (Method method : methods) {
                Log.d(TAG, "set public: " + className + ", method=" + method.getName());
                MethodHook.setMethodAccessPublic(method);
            }
        }

        /*setDexClassLoader(dexCl, class1);*/
    }

    private Method[] setAccessPublic(Class<?> clazz) {
        Field[] fields2 = clazz.getDeclaredFields();
        if (fields2.length > 0) {
            String desc;
            boolean isStatic;
            for (Field field : fields2) {
                Log.d(TAG, "set public2: " + clazz.getName() + ", field=" + field.getName());
                desc = Type.getDescriptor(field.getType());
                isStatic = Modifier.isStatic(field.getModifiers());
                MethodHook.setFieldAccessPublic(field, clazz, field.getName(), desc, isStatic);
            }
        }

        Method[] methods = clazz.getDeclaredMethods();
        if (methods.length > 0) {
            for (Method method : methods) {
//                method.getDeclaringClass() todo
                Log.d(TAG, "set public2: " + clazz.getName() + ", method=" + method.getName());
                MethodHook.setMethodAccessPublic(method);
            }
        }
        return methods;
    }

    public synchronized void removeOptFile(File file) {
        File optfile = new File(mOptDir, file.getName());
        if (optfile.exists() && !optfile.delete()) {
            Log.e(TAG, optfile.getName() + " delete error.");
        }
    }

    /*private void setDexClassLoader(DexClassLoader dexCl, Class<?> clazz) {
        try {
            //noinspection JavaReflectionMemberAccess
            Field field = Class.class.getDeclaredField("classLoader");
            field.setAccessible(true);
            field.set(clazz, dexCl);
        } catch (NoSuchFieldException | IllegalArgumentException | IllegalAccessException e) {
            Log.e(TAG, "setDexClassLoader, exception: " + e.getMessage());
        }
    }*/
}

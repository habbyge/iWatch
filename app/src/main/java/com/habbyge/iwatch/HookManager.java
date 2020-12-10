package com.habbyge.iwatch;

import android.util.Log;
import android.util.Pair;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by habbyge on 2020/11/24.
 *
 * - iWatch 项目
 * iWatch 的即时生效令人印象深刻，它稍显另类，并不需要重新启动，而是在加载补丁后直接对方法进行替换就可以完成修复，
 * 然而它的使用限制也遭遇到更多的质疑。
 *
 * 1. 使用方法：
 * iWatch的sdk jar包在公司的maven私服上，业务工程直接在build.gradle中使用即可.
 *
 * 2. 版本管理方案
 * 每次 appversion 变更都会导致所有补丁被删除，如果 appversion 没有改变，则会加载已经保存好的所有补丁。
 * 然后在需要的地方调用 PatchManager 的 addPatch 方法加载新补丁，比如可以在下载补丁文件之后调用。
 *
 * 之后就是打补丁的过程了，首先生成一个apk文件，然后更改代码，在修复bug后生成另一个apk。
 * 通过官方提供的工具apkpatch，生成一个.apatch格式的补丁文件，需要提供原apk，修复后的apk，以及一个签名文件。
 * 可以直接使用命令 apkpatch 查看具体的使用方法。
 *
 * apkpatch 将两个 apk 做一次对比，然后找出不同的部分。可以看到生成的 apatch 文件，后缀改成zip再解压开，里面
 * 有一个 dex 文件。通过jadx查看一下源码，里面就是被修复的代码所在的类文件，这些更改过的类都加上了一个 _CF 的后缀，
 * 并且变动的方法都被加上了一个叫 @MethodReplace 的 annotation，通过 clazz 和 method 指定了需要替换的方法。
 * 然后客户端sdk得到补丁文件后就会根据 annotation 来寻找需要替换的方法。最后由JNI层完成方法的替换。
 *
 * 3. iWatch 涉及的安全性问题
 * readme提示开发者需要验证下载过来的apatch文件的签名是否就是在使用apkpatch工具时使用的签名，如果不验证那么任何人
 * 都可以制作自己的  apatch 文件来对你的APP进行修改。但是我看到 iWatch 已经做了验证，如果补丁文件的证书和当前 apk
 * 的证书不是同一个的话，就不能加载补丁。
 * 官网还有一条，提示需要验证 optimize file 的指纹，应该是为了防止有人替换掉本地保存的补丁文件，所以要验证MD5码，
 * 然而 SecurityChecker 类里面也已经做了这个工作。这个 MD5 码是保存在 sharedpreference 里面.
 *
 * - odex 是个啥？
 * 将 APK 中的 classes.dex 文件通过 dex 优化过程，将其优化生成一个 ·odex 文件单独存放，原 apk 文件中的
 * classes.dex 文件可以保留，也可以删除.
 * 这样做可以加快软件的启动速度，预先提取，减少对RAM的占用，因为没有 odex 的话，系统要从apk包中提取dex再运行.
 *
 */
public final class HookManager {
    private static final String TAG = "iWatch.HookManager";

    private HookManager(){
    }

    public static HookManager get() {
        return InstanceHolder.sInstance;
    }

    private static class InstanceHolder {
        private static final HookManager sInstance = new HookManager();
    }

    private final Map<Pair<String, String>, MethodHook> methodHookMap = new ConcurrentHashMap<>();

    public void hookField(Field src, Field dst) {
        MethodHook.hookField2(src, dst);
    }

    public void fixMethod(Class<?> clazz1, String methodName1, Class<?>[] parameterTypes1,
                          Class<?> clazz2, String methodName2, Class<?>[] parameterTypes2) {

        if (clazz1 == null || methodName1 == null  || clazz2 == null || methodName2 == null) {
            Log.e(TAG, "fixMethod, error: params is NULL !");
            return;
        }

        try {
            Method sm = clazz1.getDeclaredMethod(methodName1, parameterTypes1);
            Method dm = clazz2.getDeclaredMethod(methodName2, parameterTypes2);
            HookManager.get().hookMethod(sm, dm);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    public void hookMethod(Method originMethod, Method hookMethod) {
        if (originMethod == null || hookMethod == null) {
            throw new IllegalArgumentException("argument cannot be null");
        }
//        if (!Modifier.isStatic(hookMethod.getModifiers())) {
//            throw new IllegalArgumentException("hook method must be static");
//        }
        Pair<String, String> key = Pair.create(hookMethod.getDeclaringClass().getName(),
                                               hookMethod.getName());

        if (methodHookMap.containsKey(key)) {
            MethodHook methodHook = methodHookMap.get(key);
            if (methodHook != null) {
                methodHook.restore();
            }
        }
        MethodHook methodHook = new MethodHook(originMethod, hookMethod);
        methodHookMap.put(key, methodHook);
        methodHook.hook();
    }

    public void callOrigin(Object receiver, Object... args) {
        StackTraceElement stackTrace = Thread.currentThread().getStackTrace()[3];
        String className = stackTrace.getClassName();
        String methodName = stackTrace.getMethodName();
        MethodHook methodHook = methodHookMap.get(Pair.create(className, methodName));
        if (methodHook != null) {
            try {
                methodHook.callOrigin(receiver, args);
            } catch (InvocationTargetException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }
}

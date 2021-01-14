package com.habbyge.iwatch;

import android.os.Build;
import android.util.Log;

import androidx.annotation.Keep;

import com.habbyge.iwatch.util.ReflectUtil;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Created by habbyge on 2020/11/25.
 * <p>
 * 为何选择 Native 层方案，而不是 tinker、robust 方案？
 * 1. Tinker，发补丁流程复杂，且申请复杂(其他组控制补丁版本节奏)，版本号数量有限，主要用于解决业务bug fix；
 * 2. Q-Zone 等类似 nuwa 等基于 dexElements[dex] 原理的方案：同理，数据统计补丁要想不影响 tinker 补丁的发布，
 * 就不能影响到Java代码层本身，试想，如果数据统计补丁先生效了，就会把当前的Java代码修改了，那么 tinker 补丁在下
 * 发下来后，在合成阶段，就会出坑，因为 base 版本的代码不同了.
 * 3. Robust，基于 Instant Run 原理，使用 gradle plugin 向每个方法前插入一段逻辑代码，修复bug后的代码逻辑，通过
 * DexClassLoader 加载到内存中后，并反射创建对象，赋值给目标方法中的逻辑对象，即可替换为补丁包中的新方法.
 * 按讲这个方案是对 DexClassloader 正常使用，兼容性也好，且也是下载后即可生效，那么我何为没有选择呐？
 * 我认为升级版的AndFix，即 iWatch 更简单、且没有负担，即 Robust 在编译时对每个方法都预插桩了一段逻辑代码，这样
 * 的做法不是很优雅和浪费，而且会造成内联失效，数据统计补丁的要求只是为了在某些方法中插入统计代码，并不需要在每一个
 * 方法中都插桩，因此 Robust 会造成大量浪费，这里显然不适合需求。同时 iWatch 也没有版本兼容性问题，即时生效，拥有
 * Robust 一样的能力和稳定性，同时更加轻便，所以我认为更合适.
 * 所以，这里不能对原始代码做修改，那么显而易见，就只能通过改变 art 虚拟机中的指针，来指向统计补丁代码.
 */
@Keep
public final class MethodHook {
    private static final String TAG = "iWatch.MethodHook";

//    private long backupOriMethod = -1L;
//    private Method oriMethod = null;

    private MethodHook() {
    }

    public static void init() {
        setCurThread();
        ArtMethodSize.init(Build.VERSION.SDK_INT);
    }

    public static boolean hookMethod1(String className1, String name1, String sig1,
                                      Method method1, Method method2) {

        if (method1 == null || method2 == null) {
            return false;
        }
        final long backupOriMethod = hookMethod(className1, name1, sig1, method1, method2);
        return backupOriMethod != 0L && backupOriMethod != -1L;
    }

    public static boolean hookMethod2(String className1, String funcName1, String funcSig1,
                                      boolean isStatic1, String className2, String funcName2,
                                      String funcSig2, boolean isStatic2) {

        final long backupOriMethod = hookMethod(className1, funcName1, funcSig1, isStatic1,
                                                className2, funcName2, funcSig2, isStatic2);

        return backupOriMethod != 0L && backupOriMethod != -1L;
    }

    /**
     * 方法恢复机制：
     * 1. 进程重启，初始化时，检查补丁支持的 reversion 是否与当前 app 的 reversion 相同
     * 2. 每个方法 hook 时，检查从补丁支持的 reversion 是否与当前 app 的 reversion 相同，从补丁中获取的方法
     *    是否 替换 原始方法（由注解决定）.
     * 当前 app 进程在内存中存储了目前已经被 hook 的方法列表，分为两种情况:
     * 1. 进程重启，完全不支持，则直接退出即可；
     * 2. 正在 fix 一个方法，发现补丁支持的 reversion 与当前 app 的 reversion 不符，则也恢复所有的方法，
     *    并删掉补丁文件.
     */
    // TODO: 1/15/21 恢复单独一个方法时，是需要继续执行原始函数的
    public static void restoreMethod(String className, String name, String sig) {
        unhookMethod(className, name, sig);
    }

    // TODO: 1/15/21 ing......
    public static void restoreAllMethod() {
        unhookAllmethod();
    }

    public static void hookField2(Field src, Field dst) {
        hookField(src, dst);
    }

    public static void hookClass2(String clazzName) {
        long addr = hookClass(clazzName.replace(".", "/"));
        Log.i(TAG, "hookClass2, addr=" + addr);
    }

    public static void setCurThread() {
        long threadNativeAddr = ReflectUtil.getLongField(Thread.currentThread(), "nativePeer");
        Log.i(TAG, "threadNativeAddr=" + threadNativeAddr);
        setCurThread(threadNativeAddr);
    }

    @Keep
    public static native void init(int sdkVersionCode, Method m1, Method m2);
    @Keep
    private static native long hookMethod(String srcClass, String srcName, String srcSig,
                                          Method src, Method dst);
    @Keep
    private static native long hookMethod(String className1, String funcName1, String funcSig1,
                                          boolean isStatic1, String className2, String funcName2,
                                          String funcSig2, boolean isStatic2);
    @Keep
    private static native void unhookMethod(String className, String name, String sig);
    @Keep
    private static native void unhookAllmethod();

    @Keep
    private static native long hookField(Field src, Field dst);
    @Keep
    private static native long hookClass(String className);
    @Keep
    private static native void setCurThread(long threadAddr);

    static {
        System.loadLibrary("iWatch");
    }
}

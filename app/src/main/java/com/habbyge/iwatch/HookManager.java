package com.habbyge.iwatch;

import android.util.Pair;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by habbyge on 2020/11/24.
 */
public final class HookManager {

    private HookManager(){
    }

    public static HookManager get() {
        return InstanceHolder.sInstance;
    }

    private static class InstanceHolder {
        private static final HookManager sInstance = new HookManager();
    }

    private final Map<Pair<String, String>, MethodHook> methodHookMap
            = new ConcurrentHashMap<>();

    public void hookField(Field src, Field dst) {
        MethodHook.hookField2(src, dst);
    }

    public void fixMethod(Class<?> clazz1, String methodName1, Class<?>[] parameterTypes1,
                          Class<?> clazz2, String methodName2, Class<?>[] parameterTypes2) {

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
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }
}

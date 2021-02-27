package com.habbyge.iwatch.util;

import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class ReflectUtil {
    private static final String TAG = "iWatch.ReflectUtil";

    private ReflectUtil() {
    }

    public static Field findField(Class<?> clazz, String fieldName) {
        try {
            Field field = findFieldRecursiveImpl(clazz, fieldName);
            field.setAccessible(true);
            return field;
        } catch (NoSuchFieldException e) {
            throw new NoSuchFieldError(e.getMessage());
        }
    }

    public static long getLongField(Object obj, String fieldName) {
        try {
            return findField(obj.getClass(), fieldName).getLong(obj);
        } catch (IllegalAccessException e) {
            throw new IllegalAccessError(e.getMessage());
        }
    }

    public static Method findMethod(ClassLoader cl, String className, String methodName, Class<?>... parameterTypes) {
        try {
            Class<?> _class = Class.forName(className, true, cl);
            return findMethod(_class, methodName, parameterTypes);
        } catch (ClassNotFoundException e) {
            Log.e(TAG, "findMethod-1", e);
            return null;
        }
    }

    public static Method findMethod(Class<?> clazz, String methodName, Class<?>... parameterTypes) {
        try {
            Method method = clazz.getDeclaredMethod(methodName, parameterTypes);
            method.setAccessible(true);
            return method;
        } catch (NoSuchMethodException e) {
            /*Log.e(TAG, "findMethod-2", e);*/
            return null;
        }
    }

    private static Field findFieldRecursiveImpl(Class<?> clazz, String fieldName)
            throws NoSuchFieldException {

        try {
            return clazz.getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            while (true) {
                clazz = clazz.getSuperclass();
                if (clazz == null || clazz.equals(Object.class))
                    break;

                try {
                    return clazz.getDeclaredField(fieldName);
                } catch (NoSuchFieldException ignored) {
                }
            }
            throw e;
        }
    }
}

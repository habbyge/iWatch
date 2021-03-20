package com.habbyge.iwatch.util;

import java.lang.reflect.Field;

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

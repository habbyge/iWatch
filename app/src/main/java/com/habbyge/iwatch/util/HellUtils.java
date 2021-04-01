package com.habbyge.iwatch.util;

import android.util.Log;

import java.io.File;
import java.lang.reflect.Field;

public final class HellUtils {
    private static final String TAG = "iWatch.HellUtils";
    private HellUtils() {
    }

    public static long getLongField(Object obj, String fieldName) {
        try {
            Field field = findField(obj.getClass(), fieldName);
            if (field == null) {
                return -1L;
            }
            return field.getLong(obj);
        } catch (IllegalAccessException e) {
            Log.e(TAG, "getLongField exception=" + e.getMessage());
            return -1L;
        }
    }

    private static Field findField(Class<?> clazz, String fieldName) {
        Field field = findFieldImpl(clazz, fieldName);
        if (field == null) {
            return null;
        }
        field.setAccessible(true);
        return field;
    }

    private static Field findFieldImpl(Class<?> clazz, String fieldName) {
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
            Log.e(TAG, "findFieldRecursiveImpl exception=" + e.getMessage());
            return null;
        }
    }

    public static boolean deleteFile(File file) {
        if (!file.exists()) {
            return true;
        }
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files == null) {
                return true;
            }
            for (File f : files) {
                deleteFile(f);
            }
        }
        return file.delete();
    }
}

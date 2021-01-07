package com.habbyge.iwatch.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class ReflectUtil {
    public static final ClassLoader BOOTCLASSLOADER = ClassLoader.getSystemClassLoader();

    /**
     * <p>The package separator character: <code>'&#x2e;' == {@value}</code>.</p>
     */
    public static final char PACKAGE_SEPARATOR_CHAR = '.';

    /**
     * <p>The package separator String: {@code "&#x2e;"}.</p>
     */
    public static final String PACKAGE_SEPARATOR = String.valueOf(PACKAGE_SEPARATOR_CHAR);

    /**
     * <p>The inner class separator character: <code>'$' == {@value}</code>.</p>
     */
    public static final char INNER_CLASS_SEPARATOR_CHAR = '$';

    /**
     * <p>The inner class separator String: {@code "$"}.</p>
     */
    public static final String INNER_CLASS_SEPARATOR = String.valueOf(INNER_CLASS_SEPARATOR_CHAR);

    private ReflectUtil() {
    }

    public static Class<?> findClass(String className, ClassLoader classLoader) {
        if (classLoader == null)
            classLoader = BOOTCLASSLOADER;
        try {
            return getClass(classLoader, className, false);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    // Class loading
    // ----------------------------------------------------------------------
    /**
     * Returns the class represented by {@code className} using the
     * {@code classLoader}.  This implementation supports the syntaxes
     * "{@code java.util.Map.Entry[]}", "{@code java.util.Map$Entry[]}",
     * "{@code [Ljava.util.Map.Entry;}", and "{@code [Ljava.util.Map$Entry;}".
     *
     * @param classLoader  the class loader to use to load the class
     * @param className  the class name
     * @param initialize  whether the class must be initialized
     * @return the class represented by {@code className} using the {@code classLoader}
     * @throws ClassNotFoundException if the class is not found
     */
    public static Class<?> getClass(ClassLoader classLoader, String className,
                                    boolean initialize) throws ClassNotFoundException {

        try {
            return Class.forName(toCanonicalName(className), initialize, classLoader);
        } catch (ClassNotFoundException ex) {
            // allow path separators (.) as inner class name separators
            int lastDotIndex = className.lastIndexOf(PACKAGE_SEPARATOR_CHAR);

            if (lastDotIndex != -1) {
                try {
                    return getClass(classLoader, className.substring(0, lastDotIndex) +
                                    INNER_CLASS_SEPARATOR_CHAR + className.substring(lastDotIndex + 1),
                                    initialize);
                } catch (ClassNotFoundException ex2) { // NOPMD
                    // ignore exception
                }
            }

            throw ex;
        }
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

    public static Method findMethod(String className, String methodName, Class<?>... parameterTypes) {
        try {
            Class<?> _class = Class.forName(className);
            return findMethod(_class, methodName, parameterTypes);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Method findMethod(Class<?> clazz, String methodName, Class<?>... parameterTypes) {
        try {
            Method method = clazz.getDeclaredMethod(methodName, parameterTypes);
            method.setAccessible(true);
            return method;
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
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
                } catch (NoSuchFieldException ignored) {}
            }
            throw e;
        }
    }

    /**
     * Converts a class name to a JLS style class name.
     *
     * @param className  the class name
     * @return the converted name
     */
    private static String toCanonicalName(String className) {
        className = StringUtil.deleteWhitespace(className);
        if (className == null) {
            throw new NullPointerException("className must not be null.");
        } else if (className.endsWith("[]")) { // 数组
            StringBuilder classNameBuffer = new StringBuilder();
            while (className.endsWith("[]")) { // 可能是多重数组
                className = className.substring(0, className.length() - 2);
                classNameBuffer.append("[");
            }
            classNameBuffer.append("L").append(className).append(";");
            className = classNameBuffer.toString();
        }
        return className;
    }

    /**
     * Thrown when a class loader is unable to find a class. Unlike {@link ClassNotFoundException},
     * callers are not forced to explicitly catch this. If uncaught, the error will be passed to the
     * next caller in the stack.
     */
    public static final class ClassNotFoundError extends Error {
        private static final long serialVersionUID = -1070936889459514628L;

        public ClassNotFoundError(Throwable cause) {
            super(cause);
        }

        public ClassNotFoundError(String detailMessage, Throwable cause) {
            super(detailMessage, cause);
        }
    }
}

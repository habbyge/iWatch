package com.habbyge.iwatch.util;

public final class Type {
    private Type() {
    }

    public static String getMethodDesc(final Class<?> returnType, final Class<?>... paramTypes) {
        StringBuilder buf = new StringBuilder();
        buf.append('(');
        if (paramTypes != null) {
            for (Class<?> paramType : paramTypes) {
                getDesc(buf, paramType);
            }
        }
        buf.append(')');

        getDesc(buf, returnType);
        return buf.toString();
    }

    public static String getDesc(final Class<?> clazz) {
        StringBuilder buf = new StringBuilder();
        getDesc(buf, clazz);
        return buf.toString();
    }

    private static void getDesc(final StringBuilder buf, final Class<?> c) {
        Class<?> d = c;

        while (true) {
            if (d == null) {
                return;
            }

            if (d.isPrimitive()) {
                char car;
                if (d == Integer.TYPE) {
                    car = 'I';
                } else if (d == Void.TYPE) {
                    car = 'V';
                } else if (d == Boolean.TYPE) {
                    car = 'Z';
                } else if (d == Byte.TYPE) {
                    car = 'B';
                } else if (d == Character.TYPE) {
                    car = 'C';
                } else if (d == Short.TYPE) {
                    car = 'S';
                } else if (d == Double.TYPE) {
                    car = 'D';
                } else if (d == Float.TYPE) {
                    car = 'F';
                } else /* if (d == Long.TYPE) */{
                    car = 'J';
                }
                buf.append(car);
                return;
            } else if (d.isArray()) {
                buf.append('[');
                d = d.getComponentType();
            } else {
                buf.append('L');
                String name = d.getName();
                int len = name.length();
                for (int i = 0; i < len; ++i) {
                    char car = name.charAt(i);
                    buf.append(car == '.' ? '/' : car);
                }
                buf.append(';');
                return;
            }
        }
    }
}

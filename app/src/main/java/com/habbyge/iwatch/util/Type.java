package com.habbyge.iwatch.util;

public final class Type {
    public static final int ARRAY = 9;
    public static final int OBJECT = 10;

    private final int sort;
    private final char[] buf;
    private final int off;
    private final int len;

    private Type(final int sort, final char[] buf, final int off, final int len) {
        this.sort = sort;
        this.buf = buf;
        this.off = off;
        this.len = len;
    }

    public String getDescriptor() {
        StringBuilder buf = new StringBuilder();
        getDescriptor(buf);
        return buf.toString();
    }

    public static String getMethodDescriptor(final Class<?> returnType, final Class<?>... paramTypes) {
        StringBuilder buf = new StringBuilder();
        buf.append('(');
        if (paramTypes != null) {
            for (Class<?> paramType : paramTypes) {
                getDescriptor(buf, paramType);
            }
        }
        buf.append(')');

        getDescriptor(buf, returnType);
        return buf.toString();
    }

    private void getDescriptor(final StringBuilder buf) {
        if (this.buf == null) {
            // descriptor is in byte 3 of 'off' for primitive types (buf ==
            // null)
            buf.append((char) ((off & 0xFF000000) >>> 24));
        } else if (sort == OBJECT) {
            buf.append('L');
            buf.append(this.buf, off, len);
            buf.append(';');
        } else { // sort == ARRAY || sort == METHOD
            buf.append(this.buf, off, len);
        }
    }

    private static void getDescriptor(final StringBuilder buf, final Class<?> c) {
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

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Type)) {
            return false;
        }
        Type t = (Type) o;
        if (sort != t.sort) {
            return false;
        }
        if (sort >= ARRAY) {
            if (len != t.len) {
                return false;
            }
            for (int i = off, j = t.off, end = i + len; i < end; i++, j++) {
                if (buf[i] != t.buf[j]) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hc = 13 * sort;
        if (sort >= ARRAY) {
            for (int i = off, end = i + len; i < end; i++) {
                hc = 17 * (hc + buf[i]);
            }
        }
        return hc;
    }

    @Override
    public String toString() {
        return getDescriptor();
    }
}

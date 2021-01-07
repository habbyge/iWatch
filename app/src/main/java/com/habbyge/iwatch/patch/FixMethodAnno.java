package com.habbyge.iwatch.patch;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 作用于 patch.apk 中的 class 中新的 fix method，告诉 iWatch 这个 fix method 的 origin method
 * 是谁，即：需要替换的原 method 是哪个.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface FixMethodAnno {
    /**
     * @return 需要被 fix 的 origin class
     */
    String _class();

    /**
     * @return 需要被 fix 的 origin method
     */
    String method();

    /**
     *  被fix的原始函数是否 static
     */
    boolean isStatic();
}

package com.habbyge.iwatch.patch;

import androidx.annotation.Keep;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Keep
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface FixMethodAnno {
    /**
     * @return 需要被 fix 的 origin class
     */
    String clazz();

    /**
     * @return 需要被 fix 的 origin method
     */
    String method();
}

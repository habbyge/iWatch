package com.habbyge.iwatch.patch;

import androidx.annotation.Keep;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Keep
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.CLASS)
public @interface StopSyntheticAnno {
}

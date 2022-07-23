package com.zm.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 具有该注解的实体类将会在启动的时候进行扫描
 *
 * @author ming
 * @date 2022/7/20 21:29
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface EntityHandler {
}
package com.zm.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;

/**
 * 插入时为有该注解的自动设置当前时间
 *
 * @author ming
 * @date 2022/7/10 18:29
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(value = FIELD)
public @interface CreatedDate {
}
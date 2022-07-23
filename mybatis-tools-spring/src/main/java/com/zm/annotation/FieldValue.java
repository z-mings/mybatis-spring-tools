package com.zm.annotation;

import com.zm.enums.ValueType;
import com.zm.interfaces.CustomizeProvider;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;

/**
 * @author ming
 * @date 2022/7/22 20:26
 */
@Target(value = FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface FieldValue {

    /**
     * 字段设置值的类型
     */
    ValueType valueType();

    /**
     * 自定义类型所提供值的类
     */
    Class<?> customizeClass() default CustomizeProvider.class;
}
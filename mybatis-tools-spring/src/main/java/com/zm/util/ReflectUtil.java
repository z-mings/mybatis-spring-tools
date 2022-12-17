package com.zm.util;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author ming
 * @date 2022/7/10 20:07
 */
public final class ReflectUtil {

    private ReflectUtil() {
    }

    /**
     * 获取当前类所有的字段，包括所继承的字段
     */
    public static List<Field> getFields(Class<?> clazz) {
        if (clazz == null) {
            return Collections.emptyList();
        }
        Class<?> searchClass = clazz;
        List<Field> fieldList = new ArrayList<>();
        while (searchClass != Object.class) {
            Field[] fieldArray = searchClass.getDeclaredFields();
            fieldList.addAll(Arrays.asList(fieldArray));
            searchClass = searchClass.getSuperclass();
        }
        return fieldList;
    }
}
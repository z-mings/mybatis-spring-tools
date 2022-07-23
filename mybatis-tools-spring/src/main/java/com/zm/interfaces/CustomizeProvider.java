package com.zm.interfaces;

import com.zm.annotation.FieldValue;
import com.zm.enums.ValueType;

/**
 * * 实现该接口可以在有{@link FieldValue}注解并且设置{@link ValueType}为customize的字段上自动注入值
 *
 * @author ming
 * @date 2022/7/22 20:37
 */
public interface CustomizeProvider {

    /**
     * 获取自定义的值
     *
     * @return 自定义值
     */
    Object getCustomizeValue();
}
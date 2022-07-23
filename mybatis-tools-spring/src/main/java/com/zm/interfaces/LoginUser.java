package com.zm.interfaces;

import com.zm.annotation.FieldValue;
import com.zm.enums.ValueType;

/**
 * 实现该接口可以在有{@link FieldValue}注解并且设置{@link ValueType}为createdBy或updateBy的字段上自动注入值
 *
 * @author ming
 * @date 2022/7/10 19:18
 */
public interface LoginUser {

    /**
     * 获取当前登录用户的信息
     *
     * @return 当前用户信息
     */
    String getLoginUser();
}
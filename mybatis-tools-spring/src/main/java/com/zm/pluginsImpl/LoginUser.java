package com.zm.pluginsImpl;

import com.zm.annotation.CreatedBy;
import com.zm.annotation.UpdateBy;

/**
 * 实现该接口可以在有{@link CreatedBy}和{@link UpdateBy}注解的字段上自动注入值
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
package com.zm.interfaces;

import com.zm.annotation.EntityHandler;
import com.zm.annotation.FieldValue;
import com.zm.plugins.FieldInfo;

import java.util.List;
import java.util.Map;

/**
 * @author ming
 * @date 2022/7/20 21:54
 */
public interface ScanEntityService {

    /**
     * 获取类上具有{@link EntityHandler}注解并且字段上存在{@link FieldValue}注解的实体类及相应字段
     *
     * @return 实体类及相应字段集合
     */
    Map<Class<?>, List<FieldInfo>> getScanEntity();
}
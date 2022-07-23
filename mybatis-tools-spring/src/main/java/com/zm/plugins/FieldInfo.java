package com.zm.plugins;

import com.zm.annotation.FieldValue;
import org.apache.ibatis.reflection.invoker.Invoker;

/**
 * @author ming
 * @date 2022/7/20 21:52
 */
public class FieldInfo {

    private final String fieldName;
    private final FieldValue fieldValue;
    private final Invoker getInvoker;
    private final Invoker setInvoker;

    public FieldInfo(String fieldName, FieldValue fieldValue, Invoker getInvoker, Invoker setInvoker) {
        this.fieldName = fieldName;
        this.fieldValue = fieldValue;
        this.getInvoker = getInvoker;
        this.setInvoker = setInvoker;
    }

    public String getFieldName() {
        return fieldName;
    }

    public FieldValue getFieldValue() {
        return fieldValue;
    }

    public Invoker getGetInvoker() {
        return getInvoker;
    }

    public Invoker getSetInvoker() {
        return setInvoker;
    }
}
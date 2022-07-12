package com.zm.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author ming
 * @date 2022/7/10 21:51
 */
@Component
@ConfigurationProperties(prefix = "mybatis")
public class ToolsProperties {

    /**
     * 是否开启自动设置字段值
     */
    private boolean autoSetField = true;

    public boolean getAutoSetField() {
        return autoSetField;
    }

    public void setAutoSetField(boolean autoSetField) {
        this.autoSetField = autoSetField;
    }
}
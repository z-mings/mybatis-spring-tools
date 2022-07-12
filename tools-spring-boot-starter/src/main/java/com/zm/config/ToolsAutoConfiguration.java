package com.zm.config;

import com.zm.pluginsImpl.FieldPlugin;
import com.zm.pluginsImpl.LoginUser;
import com.zm.util.BatchUtil;
import org.apache.ibatis.logging.Log;
import org.apache.ibatis.logging.LogFactory;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author ming
 * @date 2022/7/10 16:41
 */
@Configuration
public class ToolsAutoConfiguration {

    private final Log log = LogFactory.getLog(ToolsAutoConfiguration.class);

    @Bean
    public BatchUtil batchUtil(SqlSessionFactory sqlSessionFactory) {
        return new BatchUtil(sqlSessionFactory);
    }

    @Bean
    @ConditionalOnMissingBean
    public LoginUser loginUser() {
        log.warn("没有实现LoginUser接口，将无法自动设置登录人的信息");
        return () -> null;
    }

    @Bean
    @ConditionalOnProperty(name = "mybatis.autoSetField", havingValue = "true", matchIfMissing = true)
    public FieldPlugin fieldPlugin(LoginUser loginUser) {
        return new FieldPlugin(loginUser);
    }
}
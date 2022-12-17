package com.zm.config;

import com.zm.batch.BatchUpdate;
import com.zm.interfaces.CustomizeProvider;
import com.zm.interfaces.LoginUser;
import com.zm.interfaces.ScanEntityService;
import com.zm.plugins.FieldPlugin;
import com.zm.scan.EntityScanHandler;
import org.apache.ibatis.logging.Log;
import org.apache.ibatis.logging.LogFactory;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.util.List;

/**
 * @author ming
 * @date 2022/7/10 16:41
 */
@Configuration
@Import(EntityScanHandler.class)
public class ToolsAutoConfiguration {

    private final Log log = LogFactory.getLog(ToolsAutoConfiguration.class);

    @Bean
    @ConditionalOnBean(SqlSessionTemplate.class)
    public BatchUpdate batchUtil(SqlSessionTemplate sqlSessionTemplate) {
        return new BatchUpdate(sqlSessionTemplate);
    }

    @Bean
    @ConditionalOnMissingBean
    public LoginUser loginUser() {
        log.warn("没有实现LoginUser接口，将无法自动设置登录人的信息");
        return () -> null;
    }

    @Bean
    @ConditionalOnMissingBean
    public ScanEntityService scanEntityService() {
        return EntityScanHandler::getScanEntity;
    }

    @Bean
    @ConditionalOnProperty(name = "mybatis.autoSetField", havingValue = "true", matchIfMissing = true)
    public FieldPlugin fieldPlugin(LoginUser loginUser,
                                   List<CustomizeProvider> customizeProviders,
                                   ScanEntityService scanEntityService) {
        return new FieldPlugin(loginUser, customizeProviders, scanEntityService);
    }
}
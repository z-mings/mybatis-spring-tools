package com.zm.config;

import com.zm.util.BatchUtil;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author ming
 * @date 2022/7/10 16:41
 */
@Configuration
public class ToolsAutoConfiguration {

    @Bean
    public BatchUtil batchUtil(SqlSessionFactory sqlSessionFactory) {
        return new BatchUtil(sqlSessionFactory);
    }
}
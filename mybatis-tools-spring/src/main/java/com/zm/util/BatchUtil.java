package com.zm.util;

import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.defaults.DefaultSqlSessionFactory;
import org.mybatis.spring.SqlSessionUtils;

import java.util.List;
import java.util.function.ToIntBiFunction;

import static org.mybatis.spring.SqlSessionUtils.closeSqlSession;

/**
 * @author ming
 * @date 2022/7/9 19:40
 */
public class BatchUtil {

    /**
     * 默认限制最大单次批量提交数量
     */
    private final static int DEFAULT_LIMIT = 1000;
    private final SqlSessionFactory batchSessionFactory;
    private final ExecutorType batchExecutorType = ExecutorType.BATCH;

    public BatchUtil(SqlSessionFactory sqlSessionFactory) {
        Configuration configuration = sqlSessionFactory.getConfiguration();
        batchSessionFactory = new DefaultSqlSessionFactory(configuration);
    }

    public <T, U> int batchSave(List<T> list, Class<U> mapperClazz, ToIntBiFunction<U, T> func) {
        return batchSave(list, mapperClazz, func, DEFAULT_LIMIT);
    }

    public <T, U> int batchSave(List<T> list, Class<U> mapperClazz, ToIntBiFunction<U, T> func, int limit) {
        if (list == null || list.isEmpty()) {
            return 0;
        }
        SqlSession sqlSession = SqlSessionUtils.getSqlSession(batchSessionFactory, batchExecutorType, null);
        U mapper = sqlSession.getMapper(mapperClazz);
        try {
            int i = 1;
            for (T t : list) {
                func.applyAsInt(mapper, t);
                if (i == limit) {
                    i = 1;
                    sqlSession.flushStatements();
                } else {
                    i++;
                }
            }
            return list.size();
        } finally {
            closeSqlSession(sqlSession, batchSessionFactory);
        }
    }
}
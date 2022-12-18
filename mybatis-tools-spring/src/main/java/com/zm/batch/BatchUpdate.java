package com.zm.batch;

import org.apache.commons.collections4.ListUtils;
import org.apache.ibatis.executor.BatchResult;
import org.apache.ibatis.logging.Log;
import org.apache.ibatis.logging.LogFactory;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.defaults.DefaultSqlSessionFactory;
import org.mybatis.spring.MyBatisExceptionTranslator;
import org.mybatis.spring.SqlSessionHolder;
import org.mybatis.spring.SqlSessionUtils;
import org.springframework.dao.support.PersistenceExceptionTranslator;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.BiConsumer;


/**
 * 批量插入或更新的工具类
 *
 * @author ming
 * @date 2022/7/9 19:40
 */
public class BatchUpdate {

    /**
     * 默认限制最大单次批量提交数量
     */
    private static final int DEFAULT_LIMIT = 1000;
    private final SqlSessionFactory batchSessionFactory;
    private final SqlSessionFactory sqlSessionFactory;
    private final PersistenceExceptionTranslator exceptionTranslator;

    private static final ExecutorType batchExecutorType = ExecutorType.BATCH;
    private final Log log = LogFactory.getLog(BatchUpdate.class);

    public BatchUpdate(SqlSessionFactory sqlSessionFactory) {
        this.sqlSessionFactory = sqlSessionFactory;
        Configuration configuration = sqlSessionFactory.getConfiguration();
        batchSessionFactory = new DefaultSqlSessionFactory(configuration);
        exceptionTranslator = new MyBatisExceptionTranslator(sqlSessionFactory.getConfiguration().getEnvironment().getDataSource(), true);
    }

    /**
     * @param list        要批量插入的数据集合
     * @param mapperClazz 实体类对应的mapper类
     * @param func        插入单条数据时的方法
     * @return 插入条数
     */
    public <T, U> int batchSave(Collection<T> list, Class<U> mapperClazz, BiConsumer<U, T> func) {
        return batchSave(list, mapperClazz, func, DEFAULT_LIMIT);
    }

    /**
     * @param list        要批量插入的数据集合
     * @param mapperClazz 实体类对应的mapper类
     * @param func        插入单条数据时的方法
     * @param limit       单次批量提交的最大条数
     * @return 插入条数
     */
    public <T, U> int batchSave(Collection<T> list, Class<U> mapperClazz, BiConsumer<U, T> func, int limit) {
        if (list == null || list.isEmpty()) {
            log.warn("批量更新传入数据条数为0");
            return 0;
        }
        SqlSession sqlSession = SqlSessionUtils.getSqlSession(batchSessionFactory, batchExecutorType, exceptionTranslator);
        U mapper = sqlSession.getMapper(mapperClazz);
        try {
            List<T> paramList;
            if (list instanceof List) {
                paramList = (List<T>) list;
            } else {
                paramList = new ArrayList<>(list);
            }
            List<List<T>> paramPartition = ListUtils.partition(paramList, limit);
            List<BatchResult> results = new ArrayList<>();
            for (List<T> partition : paramPartition) {
                for (T param : partition) {
                    func.accept(mapper, param);
                }
                results.addAll(sqlSession.flushStatements());
            }
            if (results.size() != paramPartition.size()) {
                //由BatchExecutor源码可知，前后sql语句不一致会影响效率影响效率，最差的情况相当于单条循环插入，这种情况主要存在动态拼接参数时
                log.warn("batchSave批量提交时存在前后sql语句不一致，请尽量保证相同的sql连在一起！");
            }
            int updateSize = 0;
            for (BatchResult result : results) {
                int[] updateCounts = result.getUpdateCounts();
                for (int updateCount : updateCounts) {
                    updateSize += updateCount;
                }
            }
            if (updateSize != list.size()) {
                log.warn("batchSave批量更新警告——预计更新条数为：" + list.size() + ",实际更新条数为：" + updateSize);
            }
            if (!SqlSessionUtils.isSqlSessionTransactional(sqlSession, batchSessionFactory)) {
                sqlSession.commit(true);
            }
            return updateSize;
        } finally {
            clearCache();
            SqlSessionUtils.closeSqlSession(sqlSession, batchSessionFactory);
        }
    }

    /**
     * 情况当前事务中其他执行器的缓存
     */
    private void clearCache() {
        SqlSessionHolder holder = (SqlSessionHolder) TransactionSynchronizationManager.getResource(sqlSessionFactory);
        if (holder != null && holder.isSynchronizedWithTransaction()) {
            SqlSession sqlSession = holder.getSqlSession();
            sqlSession.clearCache();
        }
    }
}
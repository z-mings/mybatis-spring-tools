package com.zm.plugins;

import com.zm.annotation.FieldValue;
import com.zm.enums.ValueType;
import com.zm.interfaces.CustomizeProvider;
import com.zm.interfaces.LoginUser;
import com.zm.interfaces.ScanEntityService;
import org.apache.ibatis.binding.MapperMethod;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.logging.Log;
import org.apache.ibatis.logging.LogFactory;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.reflection.invoker.Invoker;

import java.lang.reflect.InvocationTargetException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.zm.enums.ValueType.*;

/**
 * @author ming
 * @date 2022/7/10 18:43
 */
@Intercepts(@Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class}))
public class FieldPlugin implements Interceptor {

    private final static String COLLECTION = "collection";
    private final LoginUser loginUser;
    private final Map<Class<? extends CustomizeProvider>, CustomizeProvider> customizeProviderMap;
    private final Map<Class<?>, List<FieldInfo>> scanEntity;
    private final Log log = LogFactory.getLog(FieldPlugin.class);

    public FieldPlugin(LoginUser loginUser, List<CustomizeProvider> customizeProvider, ScanEntityService scanEntityService) {
        this.loginUser = loginUser;
        this.scanEntity = scanEntityService.getScanEntity();
        customizeProviderMap = customizeProvider.stream()
                .collect(Collectors.toMap(CustomizeProvider::getClass, a -> a, (a, b) -> a));
    }

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        Object[] args = invocation.getArgs();
        MappedStatement mappedStatement = (MappedStatement) args[0];
        SqlCommandType sqlCommandType = mappedStatement.getSqlCommandType();
        if (SqlCommandType.INSERT != sqlCommandType && SqlCommandType.UPDATE != sqlCommandType) {
            return invocation.proceed();
        }
        Object arg = args[1];
        if (arg instanceof MapperMethod.ParamMap) {
            MapperMethod.ParamMap<?> map = (MapperMethod.ParamMap<?>) arg;
            if (map.containsKey(COLLECTION)) {
                Object obj = map.get(COLLECTION);
                if (obj instanceof Collection) {
                    Collection<?> entityList = (Collection<?>) obj;
                    setFields(sqlCommandType, entityList);
                }
            }
        } else {
            setFields(sqlCommandType, Collections.singleton(arg));
        }
        return invocation.proceed();
    }

    private void setFields(SqlCommandType sqlCommandType, Collection<?> entities)
            throws InvocationTargetException, IllegalAccessException {
        Class<?> entityClass = entities.stream().findFirst().map(Object::getClass)
                .orElseThrow(() -> new IllegalArgumentException("更新或插入时参数不能为空"));
        List<FieldInfo> fieldInfos = scanEntity.get(entityClass);
        if (fieldInfos.isEmpty()) {
            return;
        }
        Map<Class<?>, Object[]> nowDateMap = new HashMap<>(2);
        String[] user = {loginUser.getLoginUser()};
        for (Object entity : entities) {
            for (FieldInfo fieldInfo : fieldInfos) {
                Object oldValue = fieldInfo.getGetInvoker().invoke(entity, null);
                if (oldValue == null) {
                    Invoker setInvoker = fieldInfo.getSetInvoker();
                    FieldValue fieldValue = fieldInfo.getFieldValue();
                    ValueType valueType = fieldValue.valueType();
                    if (CREATED_BY == valueType && SqlCommandType.INSERT == sqlCommandType) {
                        setInvoker.invoke(entity, user);
                    } else if (CREATED_DATE == valueType && SqlCommandType.INSERT == sqlCommandType) {
                        setInvoker.invoke(entity, getNowDate(setInvoker, nowDateMap));
                    } else if (UPDATE_BY == valueType) {
                        setInvoker.invoke(entity, user);
                    } else if (UPDATE_DATE == valueType) {
                        setInvoker.invoke(entity, getNowDate(setInvoker, nowDateMap));
                    } else if (CUSTOMIZE == valueType) {
                        CustomizeProvider customizeProvider = customizeProviderMap.get(fieldValue.customizeClass());
                        Object customizeValue = customizeProvider.getCustomizeValue();
                        setInvoker.invoke(entity, new Object[]{customizeValue});
                    }
                }
            }
        }
    }

    private Object[] getNowDate(Invoker invoker, Map<Class<?>, Object[]> nowDateMap) {
        Object[] date = nowDateMap.get(invoker.getType());
        if (date == null || date.length == 0) {
            date = new Object[]{getNowDate(invoker.getType())};
            nowDateMap.put(invoker.getType(), date);
        }
        return date;
    }

    private Object getNowDate(Class<?> clazz) {
        if (clazz == null) {
            return null;
        }
        if (clazz.isAssignableFrom(Date.class)) {
            return new Date();
        }
        if (clazz.isAssignableFrom(LocalDate.class)) {
            return LocalDate.now();
        }
        if (clazz.isAssignableFrom(LocalDateTime.class)) {
            return LocalDateTime.now();
        }
        if (clazz.isAssignableFrom(Long.class) ||
                clazz.isAssignableFrom(Long.TYPE)) {
            return System.currentTimeMillis();
        }
        if (clazz.isAssignableFrom(String.class)) {
            return LocalDateTime.now().toString();
        }
        log.warn("不支持的日期类型");
        return null;
    }
}
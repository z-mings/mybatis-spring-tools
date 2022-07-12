package com.zm.pluginsImpl;

import com.zm.annotation.CreatedBy;
import com.zm.annotation.CreatedDate;
import com.zm.annotation.UpdateBy;
import com.zm.annotation.UpdateDate;
import com.zm.reflect.ReflectUtil;
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
import org.apache.ibatis.reflection.Reflector;
import org.apache.ibatis.reflection.invoker.Invoker;
import org.springframework.util.StringUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author ming
 * @date 2022/7/10 18:43
 */
@Intercepts(@Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class}))
public class FieldPlugin implements Interceptor {

    private final static String COLLECTION = "collection";
    private final LoginUser loginUser;
    private final Map<Class<?>, FieldInfo> classMap = new ConcurrentHashMap<>();

    private final Log log = LogFactory.getLog(FieldPlugin.class);

    public FieldPlugin(LoginUser loginUser) {
        this.loginUser = loginUser;
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
                    Object entity = entityList.stream().filter(Objects::nonNull).findFirst().orElse(null);
                    initEntity(entity);
                    setField(sqlCommandType, entityList);
                }
            }
        } else {
            initEntity(arg);
            setField(sqlCommandType, Collections.singleton(arg));
        }
        return invocation.proceed();
    }

    private void setField(SqlCommandType sqlCommandType, Collection<?> entities) throws InvocationTargetException, IllegalAccessException, InstantiationException {
        Class<?> entityClass = entities.stream().findFirst().map(Object::getClass)
                .orElseThrow(() -> new IllegalArgumentException("更新或插入时参数不能为空"));
        FieldInfo fieldInfo = classMap.get(entityClass);
        if (fieldInfo == null) {
            return;
        }
        Invoker createdByMethod = fieldInfo.getCreatedBy();
        Invoker createdDateMethod = fieldInfo.getCreatedDate();
        Invoker updateByMethod = fieldInfo.getUpdateBy();
        Invoker updateDateMethod = fieldInfo.getUpdateDate();
        Class<?> createdDateType = Optional.ofNullable(createdDateMethod).map(Invoker::getType).orElse(null);
        Object[] createdDate = {getNowDate(createdDateType)};
        Class<?> updateDateType = Optional.ofNullable(updateDateMethod).map(Invoker::getType).orElse(null);
        Object[] updateDate = {getNowDate(updateDateType)};
        String[] user = {loginUser.getLoginUser()};
        for (Object entity : entities) {
            if (SqlCommandType.INSERT == sqlCommandType) {
                if (createdByMethod != null && StringUtils.hasText(user[0])) {
                    createdByMethod.invoke(entity, user);
                }
                if (createdDateMethod != null) {
                    createdDateMethod.invoke(entity, createdDate);
                }
            }
            if (updateByMethod != null && StringUtils.hasText(user[0])) {
                updateByMethod.invoke(entity, user);
            }
            if (updateDateMethod != null) {
                updateDateMethod.invoke(entity, updateDate);
            }
        }
    }

    private Object getNowDate(Class<?> clazz) throws InstantiationException, IllegalAccessException {
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
        return clazz.newInstance();
    }

    public void initEntity(Object entity) {
        if (entity == null) {
            return;
        }
        Class<?> entityClass = entity.getClass();
        if (classMap.containsKey(entityClass)) {
            return;
        }
        Invoker createdBy = null;
        Invoker createdDate = null;
        Invoker updateBy = null;
        Invoker updateDate = null;
        List<Field> fields = ReflectUtil.getFields(entityClass);
        Reflector metaClass = new Reflector(entityClass);
        boolean flag = false;
        for (Field field : fields) {
            Annotation[] annotations = field.getAnnotations();
            if (annotations.length == 0) {
                continue;
            }
            for (Annotation annotation : annotations) {
                if (annotation instanceof CreatedBy) {
                    createdBy = metaClass.getSetInvoker(field.getName());
                    flag = true;
                } else if (annotation instanceof CreatedDate) {
                    createdDate = metaClass.getSetInvoker(field.getName());
                    flag = true;
                } else if (annotation instanceof UpdateBy) {
                    updateBy = metaClass.getSetInvoker(field.getName());
                    flag = true;
                } else if (annotation instanceof UpdateDate) {
                    updateDate = metaClass.getSetInvoker(field.getName());
                    flag = true;
                }
            }
        }
        if (flag) {
            FieldInfo fieldInfo = new FieldInfo(createdBy, createdDate, updateBy, updateDate);
            classMap.put(entityClass, fieldInfo);
        }
    }

    static class FieldInfo {

        private final Invoker createdBy;
        private final Invoker createdDate;
        private final Invoker updateBy;
        private final Invoker updateDate;

        public FieldInfo(Invoker createdBy,
                         Invoker createdDate,
                         Invoker updateBy,
                         Invoker updateDate) {
            this.createdBy = createdBy;
            this.createdDate = createdDate;
            this.updateBy = updateBy;
            this.updateDate = updateDate;
        }

        public Invoker getCreatedBy() {
            return createdBy;
        }

        public Invoker getCreatedDate() {
            return createdDate;
        }

        public Invoker getUpdateBy() {
            return updateBy;
        }

        public Invoker getUpdateDate() {
            return updateDate;
        }
    }
}
package com.zm.scan;

import com.zm.annotation.EntityHandler;
import com.zm.annotation.FieldValue;
import com.zm.plugins.FieldInfo;
import com.zm.reflect.ReflectUtil;
import org.apache.ibatis.reflection.Reflector;
import org.apache.ibatis.reflection.invoker.Invoker;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author ming
 * @date 2022/7/20 21:31
 */
public class EntityScanHandler implements ImportBeanDefinitionRegistrar, BeanFactoryAware, ResourceLoaderAware {

    private static final Map<Class<?>, List<FieldInfo>> SCAN_ENTITY = new HashMap<>();
    private BeanFactory beanFactory;
    private ResourceLoader resourceLoader;

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(registry, false);
        scanner.setResourceLoader(resourceLoader);
        scanner.addIncludeFilter(new AnnotationTypeFilter(EntityHandler.class));
        List<String> packages = AutoConfigurationPackages.get(beanFactory);
        for (String aPackage : packages) {
            Set<BeanDefinition> candidateComponents = scanner.findCandidateComponents(aPackage);
            for (BeanDefinition candidateComponent : candidateComponents) {
                String className = candidateComponent.getBeanClassName();
                try {
                    Class<?> clazz = Class.forName(className, false, resourceLoader.getClassLoader());
                    setScanEntity(clazz);
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    private void setScanEntity(Class<?> clazz) {
        List<Field> fields = ReflectUtil.getFields(clazz);
        Reflector metaClass = new Reflector(clazz);
        List<FieldInfo> fieldInfos = new ArrayList<>();
        for (Field field : fields) {
            FieldValue fieldValue = field.getAnnotation(FieldValue.class);
            if (fieldValue != null) {
                String fieldName = field.getName();
                Invoker getInvoker = metaClass.getGetInvoker(fieldName);
                Invoker setInvoker = metaClass.getSetInvoker(fieldName);
                FieldInfo fieldInfo = new FieldInfo(fieldName, fieldValue, getInvoker, setInvoker);
                fieldInfos.add(fieldInfo);
            }
        }
        SCAN_ENTITY.put(clazz, Collections.unmodifiableList(fieldInfos));
    }

    public static Map<Class<?>, List<FieldInfo>> getScanEntity() {
        return Collections.unmodifiableMap(SCAN_ENTITY);
    }
}
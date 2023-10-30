package cn.veasion.project.utils;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * SpringBeanUtils
 *
 * @author luozhuowei
 * @date 2022/6/28
 */
@Configuration
@SuppressWarnings("unchecked")
public class SpringBeanUtils implements ApplicationContextAware {

    private static ApplicationContext applicationContext;
    private static final Map<Class<?>, Object> CACHE_MAP = new HashMap<>();

    public static <T> T getBean(Class<T> clazz) {
        if (CACHE_MAP.containsKey(clazz)) {
            return (T) CACHE_MAP.get(clazz);
        }
        if (applicationContext == null) {
            throw new RuntimeException("SpringBeanUtils 类 applicationContext 未初始化");
        }
        T bean = applicationContext.getBean(clazz);
        synchronized (CACHE_MAP) {
            CACHE_MAP.put(clazz, bean);
        }
        return bean;
    }

    public static <T> T getBean(String beanName) {
        if (applicationContext == null) {
            throw new RuntimeException("SpringBeanUtils 类 applicationContext 未初始化");
        }
        return (T) applicationContext.getBean(beanName);
    }

    public static <T> Map<String, T> getBeanOfType(Class<T> clazz) {
        if (applicationContext == null) {
            throw new RuntimeException("SpringBeanUtils 类 applicationContext 未初始化");
        }
        return applicationContext.getBeansOfType(clazz);
    }

    public static <T> Optional<T> getBeanOfNullable(Class<T> clazz) {
        if (applicationContext == null) {
            return Optional.empty();
        } else {
            return Optional.ofNullable(getBean(clazz));
        }
    }

    public static <T> Optional<T> getBeanOfNullable(String beanName) {
        if (applicationContext != null && applicationContext.containsBean(beanName)) {
            return Optional.of((T) applicationContext.getBean(beanName));
        } else {
            return Optional.empty();
        }
    }

    public static <T> T getProperties(String property, Class<T> clazz) {
        return getProperties(property, null, clazz);
    }

    public static <T> T getProperties(String property, T defaultValue, Class<T> clazz) {
        T result = defaultValue;
        try {
            result = getBean(Environment.class).getProperty(property, clazz);
        } catch (Exception ignored) {
        }
        return result;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        SpringBeanUtils.applicationContext = applicationContext;
    }

    public static ApplicationContext getApplicationContext() {
        return applicationContext;
    }

}

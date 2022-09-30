package cn.veasion.project.aspect;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口限流
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Limit {

    /**
     * 接口名称
     */
    String name() default "";

    /**
     * 接口 key
     */
    String key() default "";

    /**
     * 时间（秒）
     */
    int periodOfSeconds();

    /**
     * 限制访问次数
     */
    int maxCount();

    /**
     * 限制类型
     */
    LimitType limitType() default LimitType.USER_ID;

    /**
     * 拼接请求URL
     */
    boolean requestURI() default false;

    enum LimitType {
        /**
         * 用户ID
         */
        USER_ID,

        /**
         * 请求IP
         */
        IP,

        /**
         * 方法参数
         */
        METHOD_PARAM_ALL
    }

}

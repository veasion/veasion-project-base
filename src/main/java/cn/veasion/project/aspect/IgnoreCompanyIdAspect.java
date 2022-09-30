package cn.veasion.project.aspect;

import cn.veasion.project.interceptor.CompanyInterceptor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * IgnoreCompanyIdAspect
 *
 * @author luozhuowei
 */
@Aspect
@Component
public class IgnoreCompanyIdAspect {

    @Around("@within(cn.veasion.project.aspect.IgnoreCompanyId)")
    public Object around(ProceedingJoinPoint joinPoint) {
        return CompanyInterceptor.withSkip(() -> {
            try {
                return joinPoint.proceed();
            } catch (Throwable t) {
                if (t instanceof RuntimeException) {
                    throw (RuntimeException) t;
                } else {
                    throw new RuntimeException(t.getMessage(), t);
                }
            }
        });
    }

}

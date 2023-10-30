package cn.veasion.project.aspect;

import cn.veasion.project.BusinessException;
import cn.veasion.project.eval.EvalAnalysisUtils;
import cn.veasion.project.session.SessionHelper;
import cn.veasion.project.utils.RequestHolder;
import cn.veasion.project.utils.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;

/**
 * LimitAspect
 *
 * @author luozhuowei
 */
@Aspect
@Component
public class LimitAspect {

    private final RedisTemplate<String, Object> redisTemplate;

    public LimitAspect(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Pointcut("@annotation(cn.veasion.project.aspect.Limit)")
    public void pointcut() {
    }

    @Around("pointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method signatureMethod = signature.getMethod();
        Limit limit = signatureMethod.getAnnotation(Limit.class);
        StringBuilder key = new StringBuilder("limit:");
        key.append(limit.key());
        if (limit.limitType() == Limit.LimitType.IP) {
            HttpServletRequest request = RequestHolder.getHttpServletRequest();
            Objects.requireNonNull(request, "请求不能为空");
            key.append("_").append(StringUtils.getIp(request));
        } else if (limit.limitType() == Limit.LimitType.METHOD_PARAM_ALL) {
            key.append("_").append(Arrays.toString(joinPoint.getArgs()));
        } else if (limit.limitType() == Limit.LimitType.PARAM_ANNOTATION) {
            for (int i = 0; i < signatureMethod.getParameters().length; i++) {
                Parameter parameter = signatureMethod.getParameters()[i];
                LimitParam annotation = parameter.getAnnotation(LimitParam.class);
                if (annotation != null) {
                    Object arg = joinPoint.getArgs()[i];
                    String eval = annotation.value();
                    if ("".equals(eval)) {
                        key.append("_").append(arg);
                    } else {
                        key.append("_").append(EvalAnalysisUtils.parse(eval, arg));
                    }
                }
            }
        } else {
            String userId = SessionHelper.getUserId();
            if (userId != null) {
                key.append("_").append(userId);
            } else {
                HttpServletRequest request = RequestHolder.getHttpServletRequest();
                Objects.requireNonNull(request, "请求不能为空");
                key.append("_").append(StringUtils.getIp(request));
            }
        }
        if (limit.requestURI()) {
            HttpServletRequest request = RequestHolder.getHttpServletRequest();
            Objects.requireNonNull(request, "请求不能为空");
            key.append("_").append(request.getRequestURI().replace("/", "_"));
        }
        Number count = count(key.toString(), limit.maxCount(), limit.periodOfSeconds());
        if (null != count && count.longValue() <= limit.maxCount()) {
            return joinPoint.proceed();
        } else {
            throw new BusinessException("操作太频繁，请稍后再试");
        }
    }

    public Number count(String key, int maxCount, int periodOfSeconds) {
        String luaScript = buildLuaScript();
        RedisScript<Number> redisScript = new DefaultRedisScript<>(luaScript, Number.class);
        return redisTemplate.execute(redisScript, Collections.singletonList(key), maxCount, periodOfSeconds);
    }

    /**
     * 限流脚本
     */
    public static String buildLuaScript() {
        return "local c" +
                "\nc = redis.call('get', KEYS[1])" +
                "\nif c and tonumber(c) > tonumber(ARGV[1]) then" +
                "\nreturn c;" +
                "\nend" +
                "\nc = redis.call('incr', KEYS[1])" +
                "\nif tonumber(c) == 1 then" +
                "\nredis.call('expire', KEYS[1], ARGV[2])" +
                "\nend" +
                "\nreturn c;";
    }

}

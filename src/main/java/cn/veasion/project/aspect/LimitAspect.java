package cn.veasion.project.aspect;

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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
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
        HttpServletRequest request = Objects.requireNonNull(RequestHolder.getHttpServletRequest());
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method signatureMethod = signature.getMethod();
        Limit limit = signatureMethod.getAnnotation(Limit.class);
        StringBuilder key = new StringBuilder("limit:");
        key.append(limit.key());
        if (limit.limitType() == Limit.LimitType.IP) {
            key.append("_").append(StringUtils.getIp(request));
        } else if (limit.limitType() == Limit.LimitType.METHOD_PARAM_ALL) {
            key.append("_").append(Arrays.toString(joinPoint.getArgs()));
        } else {
            String userId = SessionHelper.getUserId();
            key.append("_").append(userId != null ? userId : StringUtils.getIp(request));
        }
        if (limit.requestURI()) {
            key.append("_").append(request.getRequestURI().replace("/", "_"));
        }
        List<String> keys = Collections.singletonList(key.toString());
        String luaScript = buildLuaScript();
        RedisScript<Number> redisScript = new DefaultRedisScript<>(luaScript, Number.class);
        Number count = redisTemplate.execute(redisScript, keys, limit.maxCount(), limit.periodOfSeconds());
        if (null != count && count.longValue() <= limit.maxCount()) {
            return joinPoint.proceed();
        } else {
            throw new RuntimeException("操作太频繁，请稍后再试");
        }
    }

    /**
     * 限流脚本
     */
    private static String buildLuaScript() {
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

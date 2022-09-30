package cn.veasion.project.aspect;

import cn.hutool.json.JSONUtil;
import cn.veasion.project.model.SysLogVO;
import cn.veasion.project.service.SysLogService;
import cn.veasion.project.session.SessionHelper;
import cn.veasion.project.utils.RequestHolder;
import cn.veasion.project.utils.SpringBeanUtils;
import cn.veasion.project.utils.StringUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Aspect
@Component
public class LogAspect {

    private ThreadLocal<Long> currentTime = new ThreadLocal<>();

    /**
     * 配置切入点
     */
    @Pointcut("@annotation(cn.veasion.project.aspect.Log)")
    public void logPointcut() {
    }

    /**
     * 配置环绕通知,使用在方法logPointcut()上注册的切入点
     *
     * @param joinPoint join point for advice
     */
    @Around("logPointcut()")
    public Object logAround(ProceedingJoinPoint joinPoint) throws Throwable {
        Object result;
        currentTime.set(System.currentTimeMillis());
        result = joinPoint.proceed();
        SysLogVO log = new SysLogVO();
        log.setLogType("INFO");
        log.setCreateTime(new Date());
        log.setTime(System.currentTimeMillis() - currentTime.get());
        currentTime.remove();
        saveLog(log, joinPoint);
        return result;
    }

    /**
     * 配置异常通知
     *
     * @param joinPoint join point for advice
     * @param e         exception
     */
    @AfterThrowing(pointcut = "logPointcut()", throwing = "e")
    public void logAfterThrowing(JoinPoint joinPoint, Throwable e) {
        SysLogVO log = new SysLogVO();
        log.setLogType("ERROR");
        log.setTime(System.currentTimeMillis() - currentTime.get());
        currentTime.remove();
        StringWriter sw = new StringWriter();
        try (PrintWriter pw = new PrintWriter(sw)) {
            e.printStackTrace(pw);
            log.setExceptionDetail(sw.toString());
        }
        saveLog(log, (ProceedingJoinPoint) joinPoint);
    }

    private void saveLog(SysLogVO log, ProceedingJoinPoint joinPoint) {
        HttpServletRequest request = Objects.requireNonNull(RequestHolder.getHttpServletRequest());
        String ip = StringUtils.getIp(request);
        String browser = request.getHeader("User-Agent");
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Log aopLog = method.getAnnotation(Log.class);
        String methodName = joinPoint.getTarget().getClass().getName() + "." + signature.getName() + "()";
        log.setDescription(aopLog.value());
        log.setRequestIp(ip);
        log.setMethod(methodName);
        log.setParams(getParameter(method, joinPoint.getArgs()));
        log.setBrowser(browser);
        log.setUsername(SessionHelper.getUserName());
        log.setCompanyId(SessionHelper.getCompanyId());
        SysLogService sysLogService = SpringBeanUtils.getBean(SysLogService.class);
        if (sysLogService != null) {
            sysLogService.asyncSaveLog(log);
        } else {
            throw new RuntimeException("SysLogService 未找到实现类");
        }
    }

    private String getParameter(Method method, Object[] args) {
        List<Object> argList = new ArrayList<>();
        Parameter[] parameters = method.getParameters();
        for (int i = 0; i < parameters.length; i++) {
            RequestBody requestBody = parameters[i].getAnnotation(RequestBody.class);
            if (requestBody != null) {
                argList.add(args[i]);
            }
            RequestParam requestParam = parameters[i].getAnnotation(RequestParam.class);
            if (requestParam != null) {
                Map<String, Object> map = new HashMap<>();
                String key = parameters[i].getName();
                if (!StringUtils.isEmpty(requestParam.value())) {
                    key = requestParam.value();
                }
                map.put(key, args[i]);
                argList.add(map);
            }
        }
        if (argList.isEmpty()) {
            return "";
        }
        return argList.size() == 1 ? JSONUtil.toJsonStr(argList.get(0)) : JSONUtil.toJsonStr(argList);
    }

}

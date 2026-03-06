package com.travel.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

/**
 * API请求日志切面
 * 自动记录所有Controller的请求和响应信息
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class ApiLogAspect {

    private final ObjectMapper objectMapper;

    /**
     * 切入点：所有Controller层的方法
     */
    @Pointcut("execution(* com.travel.controller..*.*(..))")
    public void controllerPointcut() {
    }

    @Around("controllerPointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();

        // 获取请求信息
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes != null ? attributes.getRequest() : null;

        String method = request != null ? request.getMethod() : "UNKNOWN";
        String uri = request != null ? request.getRequestURI() : "UNKNOWN";
        String ip = request != null ? getClientIp(request) : "UNKNOWN";

        // 获取方法信息
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String className = signature.getDeclaringType().getSimpleName();
        String methodName = signature.getName();

        // 获取请求参数（过滤敏感信息和文件参数）
        String params = getParams(joinPoint, signature);

        log.info(">>> 请求开始 | {} {} | {}.{} | IP: {} | 参数: {}",
                method, uri, className, methodName, ip, params);

        Object result;
        try {
            result = joinPoint.proceed();
            long costTime = System.currentTimeMillis() - startTime;

            // 响应日志（只记录耗时，不记录完整响应体以减少日志量）
            log.info("<<< 请求完成 | {} {} | 耗时: {}ms", method, uri, costTime);

            // 慢接口警告（超过3秒）
            if (costTime > 3000) {
                log.warn("!!! 慢接口警告 | {} {} | {}.{} | 耗时: {}ms",
                        method, uri, className, methodName, costTime);
            }

            return result;
        } catch (Exception e) {
            long costTime = System.currentTimeMillis() - startTime;
            log.error("!!! 请求异常 | {} {} | {}.{} | 耗时: {}ms | 异常: {}",
                    method, uri, className, methodName, costTime, e.getMessage());
            throw e;
        }
    }

    /**
     * 获取请求参数（过滤敏感字段和文件类型）
     */
    private String getParams(ProceedingJoinPoint joinPoint, MethodSignature signature) {
        try {
            String[] paramNames = signature.getParameterNames();
            Object[] args = joinPoint.getArgs();
            if (paramNames == null || args == null || args.length == 0) {
                return "无";
            }

            Map<String, Object> paramMap = new HashMap<>();
            for (int i = 0; i < paramNames.length; i++) {
                String name = paramNames[i];
                Object value = args[i];

                // 跳过文件参数
                if (value instanceof MultipartFile) {
                    MultipartFile file = (MultipartFile) value;
                    paramMap.put(name, "文件[" + file.getOriginalFilename() + ", " + file.getSize() + "字节]");
                    continue;
                }

                // 过滤敏感字段
                if (isSensitiveParam(name)) {
                    paramMap.put(name, "******");
                    continue;
                }

                // 跳过HttpServletRequest/Response等特殊类型
                if (value != null && isSkipType(value)) {
                    continue;
                }

                paramMap.put(name, value);
            }

            return objectMapper.writeValueAsString(paramMap);
        } catch (Exception e) {
            return "参数解析失败";
        }
    }

    /**
     * 判断是否为敏感参数
     */
    private boolean isSensitiveParam(String paramName) {
        String lower = paramName.toLowerCase();
        return lower.contains("password") || lower.contains("secret")
                || lower.contains("token") || lower.contains("key");
    }

    /**
     * 判断是否跳过的类型
     */
    private boolean isSkipType(Object value) {
        String typeName = value.getClass().getName();
        return typeName.startsWith("jakarta.servlet")
                || typeName.startsWith("org.springframework.web");
    }

    /**
     * 获取客户端真实IP
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 多级代理时取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}


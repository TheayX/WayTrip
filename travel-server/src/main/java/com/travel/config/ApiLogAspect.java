package com.travel.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.FilterProvider;
import com.fasterxml.jackson.databind.ser.PropertyWriter;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
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
import java.util.Set;

/**
 * API请求日志切面
 * 自动记录所有Controller的请求和响应信息
 */
@Slf4j
@Aspect
@Component
public class ApiLogAspect {

    /**
     * 敏感字段关键词（全小写匹配）
     */
    private static final Set<String> SENSITIVE_KEYWORDS = Set.of(
            "password", "secret", "token", "key", "authorization",
            "credential", "code", "openid"
    );

    private static final String MASK = "******";

    /**
     * 专用于日志输出的 ObjectMapper，会自动脱敏敏感字段
     */
    private ObjectMapper logObjectMapper;

    @PostConstruct
    public void init() {
        logObjectMapper = new ObjectMapper();
        logObjectMapper.findAndRegisterModules();
        // 注册敏感字段过滤序列化模块
        SimpleModule module = new SimpleModule("SensitiveMaskModule");
        logObjectMapper.registerModule(module);
        // 使用自定义过滤器：对所有 Bean 属性做脱敏
        FilterProvider filters = new SimpleFilterProvider()
                .setDefaultFilter(new SensitiveFieldFilter())
                .setFailOnUnknownId(false);
        logObjectMapper.setFilterProvider(filters);
        // 让所有类都使用默认过滤器
        logObjectMapper.setConfig(
                logObjectMapper.getSerializationConfig()
                        .withAttribute("SENSITIVE_FILTER", true)
        );
    }

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
     * 获取请求参数（深度过滤敏感字段和文件类型）
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

                // 跳过null
                if (value == null) {
                    paramMap.put(name, null);
                    continue;
                }

                // 跳过文件参数
                if (value instanceof MultipartFile file) {
                    paramMap.put(name, "文件[" + file.getOriginalFilename() + ", " + file.getSize() + "字节]");
                    continue;
                }

                // 跳过HttpServletRequest/Response等特殊类型
                if (isSkipType(value)) {
                    continue;
                }

                // 对参数名本身做脱敏（如方法参数叫password）
                if (isSensitiveParam(name)) {
                    paramMap.put(name, MASK);
                    continue;
                }

                paramMap.put(name, value);
            }

            // 用脱敏 ObjectMapper 序列化，自动过滤 DTO 内部的敏感字段
            return maskSensitiveFields(logObjectMapper.writeValueAsString(paramMap));
        } catch (Exception e) {
            return "参数解析失败";
        }
    }

    /**
     * 基于正则对序列化后的 JSON 字符串做二次兜底脱敏
     * 匹配 "password":"xxx" / "secret":"xxx" / "code":"xxx" 等
     */
    private String maskSensitiveFields(String json) {
        if (json == null) return "无";
        // 匹配 JSON 中的敏感字段值
        for (String keyword : SENSITIVE_KEYWORDS) {
            // 不区分大小写替换，兼容各种命名风格（password、Password、newPassword等）
            json = json.replaceAll(
                    "(?i)(\"[^\"]*" + keyword + "[^\"]*\"\\s*:\\s*)\"[^\"]*\"",
                    "$1\"" + MASK + "\""
            );
        }
        return json;
    }

    /**
     * 判断参数名是否包含敏感关键词
     */
    private boolean isSensitiveParam(String paramName) {
        String lower = paramName.toLowerCase();
        return SENSITIVE_KEYWORDS.stream().anyMatch(lower::contains);
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

    /**
     * Jackson 属性过滤器：序列化时自动将敏感字段值替换为 ******
     */
    private static class SensitiveFieldFilter extends SimpleBeanPropertyFilter {

        @Override
        public void serializeAsField(Object pojo, JsonGenerator jgen,
                                     SerializerProvider provider, PropertyWriter writer) throws Exception {
            String fieldName = writer.getName().toLowerCase();
            boolean isSensitive = SENSITIVE_KEYWORDS.stream().anyMatch(fieldName::contains);

            if (isSensitive) {
                jgen.writeStringField(writer.getName(), MASK);
            } else {
                writer.serializeAsField(pojo, jgen, provider);
            }
        }
    }
}


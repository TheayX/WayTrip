package com.travel.config.aspect;

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
 * API 请求日志切面。
 * <p>
 * 自动记录 Controller 请求、耗时与异常信息，并对日志中的敏感字段做统一脱敏。
 */
@Slf4j
@Aspect
@Component
public class ApiLogAspect {

    /**
     * 敏感字段关键词（全小写匹配）。
     */
    private static final Set<String> SENSITIVE_KEYWORDS = Set.of(
            "password", "secret", "token", "key", "authorization",
            "credential", "code", "openid"
    );

    private static final String MASK = "******";

    /**
     * 专用于日志输出的 ObjectMapper，会自动脱敏敏感字段。
     */
    private ObjectMapper logObjectMapper;

    /**
     * 初始化日志专用 ObjectMapper，并注册敏感字段过滤器。
     */
    @PostConstruct
    public void init() {
        logObjectMapper = new ObjectMapper();
        logObjectMapper.findAndRegisterModules();
        // 日志专用 ObjectMapper 和业务序列化链路隔离，避免为了日志脱敏影响正常接口输出。
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
     * 切入点：所有 Controller 层的方法。
     */
    @Pointcut("execution(* com.travel.controller..*.*(..))")
    public void controllerPointcut() {
    }

    /**
     * 环绕记录 Controller 请求日志、耗时和异常信息。
     */
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

        // 请求参数在切面层统一做文件过滤和敏感字段脱敏，避免业务日志各自重复处理。
        String params = getParams(joinPoint, signature);

        log.info(">>> 请求开始 | {} {} | {}.{} | IP: {} | 参数: {}",
                method, uri, className, methodName, ip, params);

        Object result;
        try {
            result = joinPoint.proceed();
            long costTime = System.currentTimeMillis() - startTime;

            // 响应日志只记录耗时，不默认打印完整响应体，避免日志膨胀和敏感数据扩散。
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
     * 获取请求参数（深度过滤敏感字段和文件类型）。
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

                // 参数名本身若带敏感关键词，优先直接打码，避免 DTO 外层字段名泄露敏感值含义。
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
     * 基于正则对序列化后的 JSON 字符串做二次兜底脱敏。
     * <p>
     * 用于匹配 `"password":"xxx"`、`"secret":"xxx"`、`"code":"xxx"` 等场景。
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
     * 判断参数名是否包含敏感关键词。
     */
    private boolean isSensitiveParam(String paramName) {
        String lower = paramName.toLowerCase();
        return SENSITIVE_KEYWORDS.stream().anyMatch(lower::contains);
    }

    /**
     * 判断是否需要跳过的参数类型。
     */
    private boolean isSkipType(Object value) {
        String typeName = value.getClass().getName();
        return typeName.startsWith("jakarta.servlet")
                || typeName.startsWith("org.springframework.web");
    }

    /**
     * 获取客户端真实 IP。
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
        // 多级代理场景下只取最前面的真实来源 IP。
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


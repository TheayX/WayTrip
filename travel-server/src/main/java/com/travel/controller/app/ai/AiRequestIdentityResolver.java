package com.travel.controller.app.ai;

import com.travel.util.security.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * AI 请求身份解析器。
 * <p>
 * 用户端 AI 相关接口允许“匿名访问 + 登录态增强”并存，因此不能直接依赖认证拦截器写入的线程上下文。
 * 这里统一收口 Bearer Token、用户身份、管理员身份与客户端 IP 的解析逻辑，避免控制器各自维护重复实现。
 */
@Component
@RequiredArgsConstructor
public class AiRequestIdentityResolver {

    /**
     * JWT 解析工具。
     */
    private final JwtUtils jwtUtils;

    /**
     * 从 HTTP 请求中解析 Bearer Token。
     *
     * @param request HTTP 请求
     * @return Token；不存在或格式非法时返回 {@code null}
     */
    public String resolveToken(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        return resolveToken(request.getHeader("Authorization"));
    }

    /**
     * 从 Authorization 请求头中提取 Bearer Token。
     *
     * @param authorization 请求头原始值
     * @return Token；不存在或格式非法时返回 {@code null}
     */
    public String resolveToken(String authorization) {
        if (!StringUtils.hasText(authorization) || !authorization.startsWith("Bearer ")) {
            return null;
        }
        String token = authorization.substring(7).trim();
        return StringUtils.hasText(token) ? token : null;
    }

    /**
     * 解析普通用户身份。
     *
     * @param request HTTP 请求
     * @return 用户 ID；解析失败时返回 {@code null}
     */
    public Long resolveUserId(HttpServletRequest request) {
        return resolveUserId(resolveToken(request));
    }

    /**
     * 解析普通用户身份。
     *
     * @param token JWT Token
     * @return 用户 ID；解析失败时返回 {@code null}
     */
    public Long resolveUserId(String token) {
        if (!StringUtils.hasText(token)) {
            return null;
        }
        return jwtUtils.getUserIdFromToken(token);
    }

    /**
     * 解析管理员身份。
     *
     * @param request HTTP 请求
     * @return 管理员 ID；解析失败时返回 {@code null}
     */
    public Long resolveAdminId(HttpServletRequest request) {
        return resolveAdminId(resolveToken(request));
    }

    /**
     * 解析管理员身份。
     *
     * @param token JWT Token
     * @return 管理员 ID；解析失败时返回 {@code null}
     */
    public Long resolveAdminId(String token) {
        if (!StringUtils.hasText(token)) {
            return null;
        }
        return jwtUtils.getAdminIdFromToken(token);
    }

    /**
     * 按常见反向代理请求头顺序解析客户端真实 IP。
     *
     * @param request HTTP 请求
     * @return 客户端 IP
     */
    public String resolveClientIp(HttpServletRequest request) {
        if (request == null) {
            return "";
        }
        String[] headers = new String[]{"X-Forwarded-For", "X-Real-IP", "Proxy-Client-IP", "WL-Proxy-Client-IP"};
        for (String header : headers) {
            String ip = request.getHeader(header);
            if (StringUtils.hasText(ip) && !"unknown".equalsIgnoreCase(ip)) {
                return ip.contains(",") ? ip.split(",")[0].trim() : ip.trim();
            }
        }
        return request.getRemoteAddr();
    }
}

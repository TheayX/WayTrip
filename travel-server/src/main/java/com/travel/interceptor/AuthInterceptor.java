package com.travel.interceptor;

import com.travel.common.exception.BusinessException;
import com.travel.common.result.ResultCode;
import com.travel.util.security.JwtUtils;
import com.travel.util.web.UserContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

/**
 * 认证拦截器
 */
@Slf4j
@Component
public class AuthInterceptor implements HandlerInterceptor {

    // Token 解析依赖
    @Autowired
    private JwtUtils jwtUtils;

    /**
     * 拦截请求并根据接口前缀写入当前用户上下文。
     */
    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler) {
        // OPTIONS 请求直接放行
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String uri = request.getRequestURI();
        String token = getToken(request);

        // 判断是用户端还是管理端
        boolean isAdmin = uri.startsWith("/api/admin/");

        if (!StringUtils.hasText(token)) {
            throw new BusinessException(ResultCode.TOKEN_INVALID);
        }

        try {
            if (isAdmin) {
                Long adminId = jwtUtils.getAdminIdFromToken(token);
                if (adminId == null) {
                    throw new BusinessException(ResultCode.TOKEN_INVALID);
                }
                UserContextHolder.setAdminId(adminId);
            } else {
                Long userId = jwtUtils.getUserIdFromToken(token);
                if (userId == null) {
                    throw new BusinessException(ResultCode.TOKEN_INVALID);
                }
                UserContextHolder.setUserId(userId);
            }
            return true;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Token 解析失败", e);
            throw new BusinessException(ResultCode.TOKEN_INVALID);
        }
    }

    @Override
    public void afterCompletion(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler, @Nullable Exception ex) {
        UserContextHolder.clear();
    }

    /**
     * 从 Authorization 请求头中提取 Bearer Token。
     */
    private String getToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}

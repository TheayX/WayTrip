package com.travel.interceptor;

import com.travel.common.exception.BusinessException;
import com.travel.common.result.ResultCode;
import com.travel.util.JwtUtil;
import com.travel.util.UserContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 认证拦截器
 */
@Slf4j
@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
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
                Long adminId = jwtUtil.getAdminIdFromToken(token);
                if (adminId == null) {
                    throw new BusinessException(ResultCode.TOKEN_INVALID);
                }
                UserContext.setAdminId(adminId);
            } else {
                Long userId = jwtUtil.getUserIdFromToken(token);
                if (userId == null) {
                    throw new BusinessException(ResultCode.TOKEN_INVALID);
                }
                UserContext.setUserId(userId);
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
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }

    private String getToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}

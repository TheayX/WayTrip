package com.travel.dto.auth.response;

import lombok.Data;
import lombok.Builder;

/**
 * 管理员登录响应对象。
 * <p>
 * 同时返回令牌和当前管理员信息，方便后台登录后直接初始化权限相关界面状态。
 */
@Data
@Builder
public class AdminLoginResponse {
    private String token;
    private Long expiresIn;
    private AdminInfo admin;
    
    @Data
    @Builder
    public static class AdminInfo {
        /**
         * 当前登录管理员的基础身份信息。
         */
        private Long id;
        private String username;
        private String realName;
    }
}

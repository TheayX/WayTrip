package com.travel.dto.auth.response;

import lombok.Data;
import lombok.Builder;

/**
 * 管理员登录响应对象。
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
        private Long id;
        private String username;
        private String realName;
    }
}

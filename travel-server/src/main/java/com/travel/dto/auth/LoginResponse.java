package com.travel.dto.auth;

import lombok.Data;
import lombok.Builder;

/**
 * 登录响应
 */
@Data
@Builder
public class LoginResponse {
    private String token;
    private Long expiresIn;
    private UserInfo user;
    
    @Data
    @Builder
    public static class UserInfo {
        private Long id;
        private String nickname;
        private String avatar;
        private String phone;
        private Boolean isNewUser;
    }
}

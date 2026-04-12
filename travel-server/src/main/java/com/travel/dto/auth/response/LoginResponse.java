package com.travel.dto.auth.response;

import lombok.Data;
import lombok.Builder;

/**
 * 通用登录响应对象。
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
        private Boolean isReactivated; // 账户是否已恢复（从停用状态）
        private Boolean isMerged; // 账户是否通过手机号密码合并（小程序绑定已有账户）
    }
}

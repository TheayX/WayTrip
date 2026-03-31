package com.travel.dto.auth.response;

import lombok.Data;
import lombok.Builder;

/**
 * 微信登录响应对象。
 * <p>
 * 新用户返回 OpenID，老用户返回 Token 和用户信息。
 */
@Data
@Builder
public class WxLoginResponse {
    private Boolean isNewUser;
    private String openid; // 新用户时返回openid，用于后续绑定手机号
    private String token; // 老用户时返回token
    private Long expiresIn;
    private LoginResponse.UserInfo user; // 老用户时返回用户信息
    private Boolean isReactivated;
}


package com.travel.dto.auth;

import lombok.Data;
import lombok.Builder;

/**
 * 微信登录响应
 * 新用户: isNewUser=true, openid有值, token为空
 * 老用户: isNewUser=false, token有值, openid为空
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


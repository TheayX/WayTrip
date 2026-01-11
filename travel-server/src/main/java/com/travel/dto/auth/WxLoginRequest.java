package com.travel.dto.auth;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;

/**
 * 微信登录请求
 */
@Data
public class WxLoginRequest {
    
    @NotBlank(message = "登录凭证不能为空")
    private String code;
}

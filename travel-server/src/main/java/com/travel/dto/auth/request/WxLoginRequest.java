package com.travel.dto.auth.request;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;

/**
 * 微信登录请求对象。
 * <p>
 * 小程序端只提交登录 code，后续会话换取和用户识别逻辑由服务端统一处理。
 */
@Data
public class WxLoginRequest {
    
    @NotBlank(message = "登录凭证不能为空")
    private String code;
}

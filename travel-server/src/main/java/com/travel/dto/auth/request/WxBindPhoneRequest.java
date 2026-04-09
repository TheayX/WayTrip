package com.travel.dto.auth.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 小程序端绑定手机号请求对象。
 * <p>
 * 同时服务绑定已有账号和新用户注册，因此保留 openid、手机号和密码三类字段。
 */
@Data
public class WxBindPhoneRequest {

    @NotBlank(message = "openid不能为空")
    private String openid;

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 50, message = "密码长度为6-50个字符")
    private String password;
}


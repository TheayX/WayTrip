package com.travel.dto.auth.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Web 端注册请求对象。
 * <p>
 * 仅承接注册首阶段所需的昵称、手机号和密码，不混入资料完善类字段。
 */
@Data
public class WebRegisterRequest {

    @Size(max = 30, message = "昵称最长30个字符")
    private String nickname;

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 50, message = "密码长度为6-50个字符")
    private String password;
}


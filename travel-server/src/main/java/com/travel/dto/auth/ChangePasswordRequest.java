package com.travel.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 修改密码请求
 */
@Data
public class ChangePasswordRequest {

    /**
     * 旧密码（微信用户首次设置密码时可为空）
     */
    private String oldPassword;

    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 50, message = "密码长度为6-50个字符")
    private String newPassword;
}


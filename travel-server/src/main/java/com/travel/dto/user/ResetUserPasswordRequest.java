package com.travel.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 管理端重置用户密码请求
 */
@Data
public class ResetUserPasswordRequest {

    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 50, message = "密码长度为6-50个字符")
    private String newPassword;
}


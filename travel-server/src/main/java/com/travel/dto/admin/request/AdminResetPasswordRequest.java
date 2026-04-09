package com.travel.dto.admin.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 管理员重置密码请求对象。
 * <p>
 * 仅承接后台重置管理员密码所需的新密码字段，避免与资料更新入口混用。
 */
@Data
public class AdminResetPasswordRequest {

    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 32, message = "密码长度需在6-32个字符之间")
    private String password;
}

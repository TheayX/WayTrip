package com.travel.dto.admin.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 管理员更新请求对象。
 * <p>
 * 用于后台编辑管理员姓名和状态等资料，不承载密码修改语义。
 */
@Data
public class AdminUpdateRequest {

    @NotBlank(message = "姓名不能为空")
    @Size(max = 32, message = "姓名长度不能超过32个字符")
    private String realName;

    @NotNull(message = "状态不能为空")
    private Integer status;
}

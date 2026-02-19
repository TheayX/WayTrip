package com.travel.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminUpdateRequest {

    @NotBlank(message = "姓名不能为空")
    @Size(max = 32, message = "姓名长度不能超过32个字符")
    private String realName;

    @NotNull(message = "状态不能为空")
    private Integer status;
}

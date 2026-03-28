package com.travel.dto.auth;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 设置偏好分类请求
 */
@Data
public class PreferencesRequest {

    private List<@NotNull(message = "分类ID不能为空") Long> categoryIds;
}

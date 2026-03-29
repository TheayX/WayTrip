package com.travel.dto.auth;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 用户偏好设置请求对象。
 */
@Data
public class PreferencesRequest {

    private List<@NotNull(message = "分类ID不能为空") Long> categoryIds;
}

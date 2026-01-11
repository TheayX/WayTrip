package com.travel.dto.auth;

import lombok.Data;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 设置偏好标签请求
 */
@Data
public class PreferencesRequest {
    
    @NotEmpty(message = "偏好标签不能为空")
    private List<String> tags;
}

package com.travel.dto.auth.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 用户偏好设置请求对象。
 * <p>
 * 偏好标签以分类 ID 集合提交，便于推荐冷启动和个人偏好维护复用同一套数据结构。
 */
@Data
public class PreferencesRequest {

    private List<@NotNull(message = "分类ID不能为空") Long> categoryIds;
}

package com.travel.dto.user.item;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 管理端用户偏好列表项。
 * <p>
 * 用于后台查看用户偏好标签、关联分类和最近更新时间等信息。
 */
@Data
@Builder
public class AdminUserPreferenceListItem {

    private Long userId;

    private String nickname;

    private String phone;

    private List<String> preferenceTags;

    private String updatedAt;

    private String createdAt;
}

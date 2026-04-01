package com.travel.dto.user.item;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 管理端用户偏好列表项。
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

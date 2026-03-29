package com.travel.dto.user;

import lombok.Data;

/**
 * 管理端用户偏好列表筛选参数。
 */
@Data
public class AdminUserPreferenceListRequest {

    private String nickname;

    private Long categoryId;

    private Integer page = 1;

    private Integer pageSize = 10;
}

package com.travel.dto.user;

import lombok.Data;

/**
 * 管理端用户列表请求
 */
@Data
public class AdminUserListRequest {

    private String nickname;

    private Integer page = 1;

    private Integer pageSize = 10;
}

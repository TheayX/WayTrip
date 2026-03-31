package com.travel.dto.user.request;

import lombok.Data;

/**
 * 管理端用户列表查询请求对象。
 */
@Data
public class AdminUserListRequest {

    private String nickname;

    private Integer page = 1;

    private Integer pageSize = 10;
}

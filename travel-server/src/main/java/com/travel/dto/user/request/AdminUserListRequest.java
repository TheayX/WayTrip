package com.travel.dto.user.request;

import lombok.Data;

import java.time.LocalDate;

/**
 * 管理端用户列表查询请求对象。
 * <p>
 * 承接后台用户列表的昵称、手机号、状态和注册时间范围筛选参数。
 */
@Data
public class AdminUserListRequest {

    private String nickname;

    private String phone;

    private Integer isDeleted;

    private LocalDate startDate;

    private LocalDate endDate;

    private Integer page = 1;

    private Integer pageSize = 10;

    private String sortBy;

    private String sortOrder;
}

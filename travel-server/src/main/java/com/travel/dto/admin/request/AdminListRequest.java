package com.travel.dto.admin.request;

import lombok.Data;

/**
 * 管理员列表查询请求对象。
 * <p>
 * 统一承接后台管理员列表的分页、关键词和状态筛选条件。
 */
@Data
public class AdminListRequest {

    private Integer page = 1;
    private Integer pageSize = 10;
    private String keyword;
    private Integer status;
}

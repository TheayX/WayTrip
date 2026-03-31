package com.travel.dto.admin.request;

import lombok.Data;

/**
 * 管理员列表查询请求对象。
 */
@Data
public class AdminListRequest {

    private Integer page = 1;
    private Integer pageSize = 10;
    private String keyword;
    private Integer status;
}

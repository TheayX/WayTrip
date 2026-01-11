package com.travel.dto.spot;

import lombok.Data;

/**
 * 管理端景点列表查询请求
 */
@Data
public class AdminSpotListRequest {
    private Integer page = 1;
    private Integer pageSize = 10;
    private String keyword;
    private Long regionId;
    private Long categoryId;
    private Integer published; // 0-未发布, 1-已发布
}

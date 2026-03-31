package com.travel.dto.spot.request;

import lombok.Data;

/**
 * 管理端景点列表筛选参数对象。
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

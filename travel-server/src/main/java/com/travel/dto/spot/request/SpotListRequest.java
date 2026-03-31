package com.travel.dto.spot.request;

import lombok.Data;

/**
 * 用户端景点列表查询参数对象。
 */
@Data
public class SpotListRequest {
    private Integer page = 1;
    private Integer pageSize = 10;
    private Long regionId;
    private Long categoryId;
    private String sortBy; // heat, rating, price_asc, price_desc
}

package com.travel.dto.spot;

import lombok.Data;

/**
 * 景点列表查询请求
 */
@Data
public class SpotListRequest {
    private Integer page = 1;
    private Integer pageSize = 10;
    private Long regionId;
    private Long categoryId;
    private String sortBy; // heat, rating, price_asc, price_desc
}

package com.travel.dto.guide;

import lombok.Data;

/**
 * 用户端穷游攻略列表查询参数对象。
 */
@Data
public class GuideBudgetListRequest {
    private Integer page = 1;
    private Integer pageSize = 8;
    private String priceMode = "under50";
    private Integer maxPrice = 50;
}

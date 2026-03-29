package com.travel.dto.guide;

import lombok.Data;

/**
 * 管理端攻略列表筛选参数对象。
 */
@Data
public class AdminGuideListRequest {
    private Integer page = 1;
    private Integer pageSize = 10;
    private String keyword;
    private String category;
    private Integer published;
}

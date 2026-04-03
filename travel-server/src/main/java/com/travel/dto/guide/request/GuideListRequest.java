package com.travel.dto.guide.request;

import lombok.Data;

/**
 * 用户端攻略列表查询参数对象。
 */
@Data
public class GuideListRequest {
    private Integer page = 1;
    private Integer pageSize = 10;
    private String keyword;
    private String category;
    private String sortBy = "time"; // time, category
}

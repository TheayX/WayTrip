package com.travel.dto.guide;

import lombok.Data;

/**
 * 攻略列表请求
 */
@Data
public class GuideListRequest {
    private Integer page = 1;
    private Integer pageSize = 10;
    private String category;
    private String sortBy = "time"; // time, category
}

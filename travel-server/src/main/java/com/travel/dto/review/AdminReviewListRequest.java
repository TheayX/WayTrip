package com.travel.dto.review;

import lombok.Data;

/**
 * 管理端评价列表筛选参数对象。
 */
@Data
public class AdminReviewListRequest {

    private String nickname;

    private String spotName;

    private Integer page = 1;

    private Integer pageSize = 10;
}

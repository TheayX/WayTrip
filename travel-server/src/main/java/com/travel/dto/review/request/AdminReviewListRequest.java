package com.travel.dto.review.request;

import lombok.Data;

import java.time.LocalDate;

/**
 * 管理端评价列表筛选参数对象。
 * <p>
 * 用于后台评价检索页接收昵称、景点、评分区间和时间范围等筛选条件。
 */
@Data
public class AdminReviewListRequest {

    private String nickname;

    private String spotName;

    private Integer minScore;

    private Integer maxScore;

    private LocalDate startDate;

    private LocalDate endDate;

    private Integer page = 1;

    private Integer pageSize = 10;
}

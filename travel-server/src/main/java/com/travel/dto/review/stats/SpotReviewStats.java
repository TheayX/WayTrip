package com.travel.dto.review.stats;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 景点评价聚合统计响应对象。
 * <p>
 * 作为评价聚合查询结果承接平均评分和评价人数等统计值。
 */
@Data
public class SpotReviewStats {

    private BigDecimal avgRating;

    private Long reviewCount;
}

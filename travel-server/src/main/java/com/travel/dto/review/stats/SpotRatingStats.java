package com.travel.dto.review.stats;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 景点评分统计响应对象。
 * <p>
 * 作为评分聚合查询结果承接平均分和评价数量等统计值。
 */
@Data
public class SpotRatingStats {

    private BigDecimal avgRating;

    private Long ratingCount;
}

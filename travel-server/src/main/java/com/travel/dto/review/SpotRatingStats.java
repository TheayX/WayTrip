package com.travel.dto.review;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 景点评分统计响应对象。
 */
@Data
public class SpotRatingStats {

    private BigDecimal avgRating;

    private Long ratingCount;
}

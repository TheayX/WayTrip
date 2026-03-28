package com.travel.dto.review;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SpotRatingStats {

    private BigDecimal avgRating;

    private Long ratingCount;
}

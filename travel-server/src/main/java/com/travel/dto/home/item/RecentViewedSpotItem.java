package com.travel.dto.home.item;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 最近都在看景点列表项。
 */
@Data
public class RecentViewedSpotItem {
    private Long id;
    private String name;
    private String coverImage;
    private BigDecimal price;
    private BigDecimal avgRating;
    private Integer heatScore;
    private String categoryName;
    private Integer viewCount;
}

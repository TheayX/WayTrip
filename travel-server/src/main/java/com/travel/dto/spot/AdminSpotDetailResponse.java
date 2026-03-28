package com.travel.dto.spot;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 管理端景点详情响应
 */
@Data
public class AdminSpotDetailResponse {

    private String name;

    private String description;

    private BigDecimal price;

    private String openTime;

    private String address;

    private BigDecimal latitude;

    private BigDecimal longitude;

    private String coverImage;

    private List<String> images;

    private Long regionId;

    private Long categoryId;

    private Boolean published;

    private BigDecimal avgRating;

    private Integer ratingCount;

    private Integer heatLevel;

    private Integer heatScore;
}

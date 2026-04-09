package com.travel.dto.spot.response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 管理端景点详情回显对象。
 * <p>
 * 面向后台编辑页，返回完整表单回填字段和附加运营指标。
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

    private String regionName;

    private Long categoryId;

    private String categoryName;

    private Long reviewCount;

    private Long favoriteCount;

    private Long viewCount;

    private Boolean published;

    private BigDecimal avgRating;

    private Integer ratingCount;

    private Integer heatLevel;

    private Integer heatScore;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}

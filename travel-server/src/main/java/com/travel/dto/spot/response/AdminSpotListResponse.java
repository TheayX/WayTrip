package com.travel.dto.spot.response;

import lombok.Data;
import lombok.Builder;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 管理端景点列表项对象。
 * <p>
 * 用于后台景点表格展示，返回列表页所需的轻量景点信息。
 */
@Data
@Builder
public class AdminSpotListResponse {
    private Long id;
    private String name;
    private String coverImage;
    private BigDecimal price;
    private String regionName;
    private String categoryName;
    private BigDecimal avgRating;
    private Integer ratingCount;
    private Integer heatLevel;
    private Integer heatScore;
    private Boolean published;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

package com.travel.dto.spot;

import lombok.Data;
import lombok.Builder;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 管理端景点列表项响应
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
    private Integer heatScore;
    private Boolean published;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

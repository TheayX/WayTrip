package com.travel.dto.spot;

import lombok.Data;
import lombok.Builder;
import java.math.BigDecimal;

/**
 * 景点列表项响应
 */
@Data
@Builder
public class SpotListResponse {
    private Long id;
    private String name;
    private String coverImage;
    private BigDecimal price;
    private BigDecimal avgRating;
    private Integer ratingCount;
    private String regionName;
    private String categoryName;
}

package com.travel.dto.spot.response;

import lombok.Data;
import lombok.Builder;
import java.math.BigDecimal;

/**
 * 用户端景点列表项响应对象。
 * <p>
 * 面向用户端景点列表和推荐卡片，返回基础展示字段和筛选辅助信息。
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

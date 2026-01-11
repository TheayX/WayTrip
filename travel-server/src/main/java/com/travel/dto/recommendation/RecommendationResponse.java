package com.travel.dto.recommendation;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 推荐响应
 */
@Data
public class RecommendationResponse {

    /**
     * 推荐类型：personalized/hot/preference
     */
    private String type;

    /**
     * 推荐列表
     */
    private List<SpotItem> list;

    /**
     * 是否需要设置偏好（冷启动提示）
     */
    private Boolean needPreference;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SpotItem {
        private Long id;
        private String name;
        private String coverImage;
        private BigDecimal price;
        private BigDecimal avgRating;
        private Integer ratingCount;
        private String categoryName;
        private String regionName;
        private Double score; // 推荐分数
    }
}

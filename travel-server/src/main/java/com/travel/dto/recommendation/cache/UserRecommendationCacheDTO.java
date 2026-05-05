package com.travel.dto.recommendation.cache;

import lombok.Data;

import java.util.List;

/**
 * 用户推荐缓存对象。
 * <p>
 * 统一缓存推荐类型、结果顺序与分数字段，便于“换一批”直接在缓存结果上做轮换。
 */
@Data
public class UserRecommendationCacheDTO {

    /**
     * 推荐类型：personalized/hot/preference
     */
    private String type;

    /**
     * 是否仍建议用户补充偏好。
     */
    private Boolean needPreference;

    /**
     * 推荐结果顺序快照。
     */
    private List<CacheItem> items;

    /**
     * 本次缓存写入时间。
     */
    private Long generatedAt;

    /**
     * 缓存中的推荐项，仅保留轮换和重建响应所需的最小字段。
     */
    @Data
    public static class CacheItem {
        private Long spotId;
        private Double score;
    }
}

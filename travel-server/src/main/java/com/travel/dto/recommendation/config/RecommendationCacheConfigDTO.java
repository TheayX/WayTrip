package com.travel.dto.recommendation.config;

import lombok.Data;

/**
 * 推荐缓存策略配置参数对象。
 */
@Data
public class RecommendationCacheConfigDTO {

    private Integer similarityTTLHours = 24;
    private Integer userRecTTLMinutes = 60;
}

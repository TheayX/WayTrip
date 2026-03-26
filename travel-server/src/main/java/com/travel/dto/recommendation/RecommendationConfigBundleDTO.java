package com.travel.dto.recommendation;

import lombok.Data;

/**
 * 聚合后的推荐配置。
 */
@Data
public class RecommendationConfigBundleDTO {

    private RecommendationAlgorithmConfigDTO algorithm = new RecommendationAlgorithmConfigDTO();
    private RecommendationHeatConfigDTO heat = new RecommendationHeatConfigDTO();
    private RecommendationCacheConfigDTO cache = new RecommendationCacheConfigDTO();

    public static RecommendationConfigBundleDTO defaultConfig() {
        return new RecommendationConfigBundleDTO();
    }
}

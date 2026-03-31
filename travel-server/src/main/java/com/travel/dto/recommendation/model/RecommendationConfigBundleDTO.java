package com.travel.dto.recommendation.model;

import lombok.Data;

/**
 * 推荐配置聚合传输对象。
 */
@Data
public class RecommendationConfigBundleDTO {

    private RecommendationAlgorithmConfigDTO algorithm = new RecommendationAlgorithmConfigDTO();
    private RecommendationHeatConfigDTO heat = new RecommendationHeatConfigDTO();
    private RecommendationCacheConfigDTO cache = new RecommendationCacheConfigDTO();

    /**
     * 构建默认推荐配置。
     *
     * @return 默认推荐配置
     */
    public static RecommendationConfigBundleDTO defaultConfig() {
        return new RecommendationConfigBundleDTO();
    }
}

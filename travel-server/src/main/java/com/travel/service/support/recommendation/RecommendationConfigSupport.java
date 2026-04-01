package com.travel.service.support.recommendation;

import com.travel.dto.recommendation.config.RecommendationConfigBundleDTO;
import com.travel.dto.recommendation.response.RecommendationStatusDTO;
import com.travel.service.cache.RecommendationCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 推荐配置与状态支撑，集中处理缓存中的配置和状态读写。
 */
@Component
@RequiredArgsConstructor
public class RecommendationConfigSupport {

    private final RecommendationCacheService recommendationCacheService;

    public RecommendationConfigBundleDTO getConfig() {
        return recommendationCacheService.loadConfig();
    }

    public void updateConfig(RecommendationConfigBundleDTO config) {
        recommendationCacheService.saveConfig(config);
    }

    @SuppressWarnings("unchecked")
    public RecommendationStatusDTO buildStatus(boolean computing) {
        RecommendationStatusDTO status = new RecommendationStatusDTO();
        status.setComputing(computing);

        Object cached = recommendationCacheService.getStatus();
        if (cached instanceof Map<?, ?> map) {
            status.setLastUpdateTime(map.get("lastUpdateTime") != null ? map.get("lastUpdateTime").toString() : null);
            status.setTotalUsers(map.get("totalUsers") instanceof Number n ? n.intValue() : null);
            status.setTotalSpots(map.get("totalSpots") instanceof Number n ? n.intValue() : null);
        }
        return status;
    }
}

package com.travel.service.support.recommendation;

import com.travel.dto.recommendation.config.RecommendationConfigBundleDTO;
import com.travel.dto.recommendation.response.RecommendationStatusDTO;
import com.travel.service.cache.RecommendationCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 推荐配置与状态支撑，集中处理缓存中的配置和状态读写。
 * <p>
 * 推荐配置和运行状态都放在这一层，便于管理端调参与状态查询复用同一份缓存入口。
 */
@Component
@RequiredArgsConstructor
public class RecommendationConfigSupport {

    private final RecommendationCacheService recommendationCacheService;

    /**
     * 读取当前推荐配置。
     *
     * @return 配置集合
     */
    public RecommendationConfigBundleDTO getConfig() {
        return recommendationCacheService.loadConfig();
    }

    /**
     * 更新推荐配置缓存。
     *
     * @param config 新配置
     */
    public void updateConfig(RecommendationConfigBundleDTO config) {
        recommendationCacheService.saveConfig(config);
    }

    @SuppressWarnings("unchecked")
    /**
     * 组合推荐引擎运行状态。
     *
     * @param computing 当前是否正在计算
     * @return 运行状态
     */
    public RecommendationStatusDTO buildStatus(boolean computing) {
        RecommendationStatusDTO status = new RecommendationStatusDTO();
        status.setComputing(computing);

        Object cached = recommendationCacheService.getStatus();
        if (cached instanceof Map<?, ?> map) {
            // 缓存里保存的是通用键值结构，这里统一转换成后台状态 DTO。
            status.setLastUpdateTime(map.get("lastUpdateTime") != null ? map.get("lastUpdateTime").toString() : null);
            status.setTotalUsers(map.get("totalUsers") instanceof Number n ? n.intValue() : null);
            status.setTotalSpots(map.get("totalSpots") instanceof Number n ? n.intValue() : null);
        }
        return status;
    }
}

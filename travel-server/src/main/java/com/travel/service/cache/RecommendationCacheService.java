package com.travel.service.cache;

import com.travel.config.AppCacheProperties;
import com.travel.config.RedisKeyManager;
import com.travel.dto.recommendation.LegacyRecommendationConfigDTO;
import com.travel.dto.recommendation.RecommendationConfigBundleDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 推荐模块缓存服务。
 */
@Service
@RequiredArgsConstructor
public class RecommendationCacheService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final AppCacheProperties appCacheProperties;

    public RecommendationConfigBundleDTO loadConfig() {
        return RecommendationConfigBundleDTO.fromLegacy(loadLegacyConfig());
    }

    public void saveConfig(RecommendationConfigBundleDTO config) {
        RecommendationConfigBundleDTO safeConfig = config == null
            ? RecommendationConfigBundleDTO.defaultConfig()
            : config;
        saveLegacyConfig(safeConfig.toLegacy());
    }

    /**
     * 读取 Redis 中的推荐配置，缺失时回退到默认值。
     */
    public LegacyRecommendationConfigDTO loadLegacyConfig() {
        LegacyRecommendationConfigDTO mergedConfig = buildDefaultConfig();
        boolean loadedFromPartitionedKeys = false;

        loadedFromPartitionedKeys |= applyConfigSection(
            mergedConfig,
            redisTemplate.opsForValue().get(RedisKeyManager.recommendationConfigAlgorithm())
        );
        loadedFromPartitionedKeys |= applyConfigSection(
            mergedConfig,
            redisTemplate.opsForValue().get(RedisKeyManager.recommendationConfigHeat())
        );
        loadedFromPartitionedKeys |= applyConfigSection(
            mergedConfig,
            redisTemplate.opsForValue().get(RedisKeyManager.recommendationConfigCache())
        );

        if (loadedFromPartitionedKeys) {
            return mergedConfig;
        }

        Object cached = redisTemplate.opsForValue().get(RedisKeyManager.recommendationConfig());
        if (cached instanceof LegacyRecommendationConfigDTO config) {
            return config;
        }
        if (cached instanceof Map<?, ?>) {
            return mapToConfig(cached);
        }
        return buildDefaultConfig();
    }

    public void saveLegacyConfig(LegacyRecommendationConfigDTO legacyConfig) {
        redisTemplate.opsForValue().set(RedisKeyManager.recommendationConfigAlgorithm(), algorithmConfigSection(legacyConfig));
        redisTemplate.opsForValue().set(RedisKeyManager.recommendationConfigHeat(), heatConfigSection(legacyConfig));
        redisTemplate.opsForValue().set(RedisKeyManager.recommendationConfigCache(), cacheConfigSection(legacyConfig));
        redisTemplate.opsForValue().set(RedisKeyManager.recommendationConfig(), legacyConfig);
    }

    public Object getUserRecommendation(Long userId) {
        return redisTemplate.opsForValue().get(RedisKeyManager.recommendationUser(userId));
    }

    public void saveUserRecommendation(Long userId, Map<Long, Double> scores, long ttlMinutes) {
        redisTemplate.opsForValue().set(
            RedisKeyManager.recommendationUser(userId),
            scores,
            ttlMinutes,
            TimeUnit.MINUTES
        );
    }

    public void deleteUserRecommendation(Long userId) {
        redisTemplate.delete(RedisKeyManager.recommendationUser(userId));
    }

    public Object getSimilarity(Long spotId) {
        return redisTemplate.opsForValue().get(RedisKeyManager.recommendationSimilarity(spotId));
    }

    public void saveSimilarity(Long spotId, Map<Long, Double> similarities, long ttlHours) {
        redisTemplate.opsForValue().set(
            RedisKeyManager.recommendationSimilarity(spotId),
            similarities,
            ttlHours,
            TimeUnit.HOURS
        );
    }

    public Object getStatus() {
        return redisTemplate.opsForValue().get(RedisKeyManager.recommendationStatus());
    }

    public void saveStatus(Map<String, Object> statusMap) {
        redisTemplate.opsForValue().set(RedisKeyManager.recommendationStatus(), statusMap);
    }

    private LegacyRecommendationConfigDTO buildDefaultConfig() {
        LegacyRecommendationConfigDTO config = LegacyRecommendationConfigDTO.defaultConfig();
        config.setUserRecTTLMinutes(defaultInt(appCacheProperties.getRecommendation().getUserRecTtlMinutes(), 60));
        config.setSimilarityTTLHours(defaultInt(appCacheProperties.getRecommendation().getSimilarityTtlHours(), 24));
        config.setHeatViewDedupeWindowMinutes(defaultInt(appCacheProperties.getSpot().getHeatViewDedupeWindowMinutes(), 30));
        return config;
    }

    private boolean applyConfigSection(LegacyRecommendationConfigDTO target, Object cached) {
        if (cached == null) {
            return false;
        }
        if (cached instanceof LegacyRecommendationConfigDTO config) {
            mergeConfig(target, config);
            return true;
        }
        if (cached instanceof Map<?, ?>) {
            mergeConfig(target, mapToConfig(cached));
            return true;
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private LegacyRecommendationConfigDTO mapToConfig(Object obj) {
        try {
            Map<String, Object> map = (Map<String, Object>) obj;
            LegacyRecommendationConfigDTO config = new LegacyRecommendationConfigDTO();
            if (map.containsKey("weightView")) config.setWeightView(toDouble(map.get("weightView")));
            if (map.containsKey("weightFavorite")) config.setWeightFavorite(toDouble(map.get("weightFavorite")));
            if (map.containsKey("weightReviewFactor")) config.setWeightReviewFactor(toDouble(map.get("weightReviewFactor")));
            if (map.containsKey("weightOrderPaid")) config.setWeightOrderPaid(toDouble(map.get("weightOrderPaid")));
            if (map.containsKey("weightOrderCompleted")) config.setWeightOrderCompleted(toDouble(map.get("weightOrderCompleted")));
            if (map.containsKey("viewSourceFactorHome")) config.setViewSourceFactorHome(toDouble(map.get("viewSourceFactorHome")));
            if (map.containsKey("viewSourceFactorSearch")) config.setViewSourceFactorSearch(toDouble(map.get("viewSourceFactorSearch")));
            if (map.containsKey("viewSourceFactorRecommend")) config.setViewSourceFactorRecommend(toDouble(map.get("viewSourceFactorRecommend")));
            if (map.containsKey("viewSourceFactorGuide")) config.setViewSourceFactorGuide(toDouble(map.get("viewSourceFactorGuide")));
            if (map.containsKey("viewSourceFactorDetail")) config.setViewSourceFactorDetail(toDouble(map.get("viewSourceFactorDetail")));
            if (map.containsKey("viewDurationShortThresholdSeconds")) config.setViewDurationShortThresholdSeconds(toInt(map.get("viewDurationShortThresholdSeconds")));
            if (map.containsKey("viewDurationMediumThresholdSeconds")) config.setViewDurationMediumThresholdSeconds(toInt(map.get("viewDurationMediumThresholdSeconds")));
            if (map.containsKey("viewDurationLongThresholdSeconds")) config.setViewDurationLongThresholdSeconds(toInt(map.get("viewDurationLongThresholdSeconds")));
            if (map.containsKey("viewDurationFactorShort")) config.setViewDurationFactorShort(toDouble(map.get("viewDurationFactorShort")));
            if (map.containsKey("viewDurationFactorMedium")) config.setViewDurationFactorMedium(toDouble(map.get("viewDurationFactorMedium")));
            if (map.containsKey("viewDurationFactorLong")) config.setViewDurationFactorLong(toDouble(map.get("viewDurationFactorLong")));
            if (map.containsKey("viewDurationFactorVeryLong")) config.setViewDurationFactorVeryLong(toDouble(map.get("viewDurationFactorVeryLong")));
            if (map.containsKey("heatViewIncrement")) config.setHeatViewIncrement(toInt(map.get("heatViewIncrement")));
            if (map.containsKey("heatFavoriteIncrement")) config.setHeatFavoriteIncrement(toInt(map.get("heatFavoriteIncrement")));
            if (map.containsKey("heatReviewIncrement")) config.setHeatReviewIncrement(toInt(map.get("heatReviewIncrement")));
            if (map.containsKey("heatOrderPaidIncrement")) config.setHeatOrderPaidIncrement(toInt(map.get("heatOrderPaidIncrement")));
            if (map.containsKey("heatOrderCompletedIncrement")) config.setHeatOrderCompletedIncrement(toInt(map.get("heatOrderCompletedIncrement")));
            if (map.containsKey("heatViewDedupeWindowMinutes")) config.setHeatViewDedupeWindowMinutes(toInt(map.get("heatViewDedupeWindowMinutes")));
            if (map.containsKey("heatRerankFactor")) config.setHeatRerankFactor(toDouble(map.get("heatRerankFactor")));
            if (map.containsKey("minInteractionsForCF")) config.setMinInteractionsForCF(toInt(map.get("minInteractionsForCF")));
            if (map.containsKey("topKNeighbors")) config.setTopKNeighbors(toInt(map.get("topKNeighbors")));
            if (map.containsKey("candidateExpandFactor")) config.setCandidateExpandFactor(toInt(map.get("candidateExpandFactor")));
            if (map.containsKey("coldStartExpandFactor")) config.setColdStartExpandFactor(toInt(map.get("coldStartExpandFactor")));
            if (map.containsKey("similarityTTLHours")) config.setSimilarityTTLHours(toInt(map.get("similarityTTLHours")));
            if (map.containsKey("userRecTTLMinutes")) config.setUserRecTTLMinutes(toInt(map.get("userRecTTLMinutes")));
            return config;
        } catch (Exception e) {
            return buildDefaultConfig();
        }
    }

    private void mergeConfig(LegacyRecommendationConfigDTO target, LegacyRecommendationConfigDTO source) {
        if (source.getWeightView() != null) target.setWeightView(source.getWeightView());
        if (source.getWeightFavorite() != null) target.setWeightFavorite(source.getWeightFavorite());
        if (source.getWeightReviewFactor() != null) target.setWeightReviewFactor(source.getWeightReviewFactor());
        if (source.getWeightOrderPaid() != null) target.setWeightOrderPaid(source.getWeightOrderPaid());
        if (source.getWeightOrderCompleted() != null) target.setWeightOrderCompleted(source.getWeightOrderCompleted());
        if (source.getViewSourceFactorHome() != null) target.setViewSourceFactorHome(source.getViewSourceFactorHome());
        if (source.getViewSourceFactorSearch() != null) target.setViewSourceFactorSearch(source.getViewSourceFactorSearch());
        if (source.getViewSourceFactorRecommend() != null) target.setViewSourceFactorRecommend(source.getViewSourceFactorRecommend());
        if (source.getViewSourceFactorGuide() != null) target.setViewSourceFactorGuide(source.getViewSourceFactorGuide());
        if (source.getViewSourceFactorDetail() != null) target.setViewSourceFactorDetail(source.getViewSourceFactorDetail());
        if (source.getViewDurationShortThresholdSeconds() != null) target.setViewDurationShortThresholdSeconds(source.getViewDurationShortThresholdSeconds());
        if (source.getViewDurationMediumThresholdSeconds() != null) target.setViewDurationMediumThresholdSeconds(source.getViewDurationMediumThresholdSeconds());
        if (source.getViewDurationLongThresholdSeconds() != null) target.setViewDurationLongThresholdSeconds(source.getViewDurationLongThresholdSeconds());
        if (source.getViewDurationFactorShort() != null) target.setViewDurationFactorShort(source.getViewDurationFactorShort());
        if (source.getViewDurationFactorMedium() != null) target.setViewDurationFactorMedium(source.getViewDurationFactorMedium());
        if (source.getViewDurationFactorLong() != null) target.setViewDurationFactorLong(source.getViewDurationFactorLong());
        if (source.getViewDurationFactorVeryLong() != null) target.setViewDurationFactorVeryLong(source.getViewDurationFactorVeryLong());
        if (source.getHeatViewIncrement() != null) target.setHeatViewIncrement(source.getHeatViewIncrement());
        if (source.getHeatFavoriteIncrement() != null) target.setHeatFavoriteIncrement(source.getHeatFavoriteIncrement());
        if (source.getHeatReviewIncrement() != null) target.setHeatReviewIncrement(source.getHeatReviewIncrement());
        if (source.getHeatOrderPaidIncrement() != null) target.setHeatOrderPaidIncrement(source.getHeatOrderPaidIncrement());
        if (source.getHeatOrderCompletedIncrement() != null) target.setHeatOrderCompletedIncrement(source.getHeatOrderCompletedIncrement());
        if (source.getHeatViewDedupeWindowMinutes() != null) target.setHeatViewDedupeWindowMinutes(source.getHeatViewDedupeWindowMinutes());
        if (source.getHeatRerankFactor() != null) target.setHeatRerankFactor(source.getHeatRerankFactor());
        if (source.getMinInteractionsForCF() != null) target.setMinInteractionsForCF(source.getMinInteractionsForCF());
        if (source.getTopKNeighbors() != null) target.setTopKNeighbors(source.getTopKNeighbors());
        if (source.getCandidateExpandFactor() != null) target.setCandidateExpandFactor(source.getCandidateExpandFactor());
        if (source.getColdStartExpandFactor() != null) target.setColdStartExpandFactor(source.getColdStartExpandFactor());
        if (source.getSimilarityTTLHours() != null) target.setSimilarityTTLHours(source.getSimilarityTTLHours());
        if (source.getUserRecTTLMinutes() != null) target.setUserRecTTLMinutes(source.getUserRecTTLMinutes());
    }

    private LegacyRecommendationConfigDTO algorithmConfigSection(LegacyRecommendationConfigDTO source) {
        LegacyRecommendationConfigDTO section = new LegacyRecommendationConfigDTO();
        section.setWeightView(source.getWeightView());
        section.setWeightFavorite(source.getWeightFavorite());
        section.setWeightReviewFactor(source.getWeightReviewFactor());
        section.setWeightOrderPaid(source.getWeightOrderPaid());
        section.setWeightOrderCompleted(source.getWeightOrderCompleted());
        section.setViewSourceFactorHome(source.getViewSourceFactorHome());
        section.setViewSourceFactorSearch(source.getViewSourceFactorSearch());
        section.setViewSourceFactorRecommend(source.getViewSourceFactorRecommend());
        section.setViewSourceFactorGuide(source.getViewSourceFactorGuide());
        section.setViewSourceFactorDetail(source.getViewSourceFactorDetail());
        section.setViewDurationShortThresholdSeconds(source.getViewDurationShortThresholdSeconds());
        section.setViewDurationMediumThresholdSeconds(source.getViewDurationMediumThresholdSeconds());
        section.setViewDurationLongThresholdSeconds(source.getViewDurationLongThresholdSeconds());
        section.setViewDurationFactorShort(source.getViewDurationFactorShort());
        section.setViewDurationFactorMedium(source.getViewDurationFactorMedium());
        section.setViewDurationFactorLong(source.getViewDurationFactorLong());
        section.setViewDurationFactorVeryLong(source.getViewDurationFactorVeryLong());
        section.setMinInteractionsForCF(source.getMinInteractionsForCF());
        section.setTopKNeighbors(source.getTopKNeighbors());
        section.setCandidateExpandFactor(source.getCandidateExpandFactor());
        section.setColdStartExpandFactor(source.getColdStartExpandFactor());
        return section;
    }

    private LegacyRecommendationConfigDTO heatConfigSection(LegacyRecommendationConfigDTO source) {
        LegacyRecommendationConfigDTO section = new LegacyRecommendationConfigDTO();
        section.setHeatViewIncrement(source.getHeatViewIncrement());
        section.setHeatFavoriteIncrement(source.getHeatFavoriteIncrement());
        section.setHeatReviewIncrement(source.getHeatReviewIncrement());
        section.setHeatOrderPaidIncrement(source.getHeatOrderPaidIncrement());
        section.setHeatOrderCompletedIncrement(source.getHeatOrderCompletedIncrement());
        section.setHeatViewDedupeWindowMinutes(source.getHeatViewDedupeWindowMinutes());
        section.setHeatRerankFactor(source.getHeatRerankFactor());
        return section;
    }

    private LegacyRecommendationConfigDTO cacheConfigSection(LegacyRecommendationConfigDTO source) {
        LegacyRecommendationConfigDTO section = new LegacyRecommendationConfigDTO();
        section.setSimilarityTTLHours(source.getSimilarityTTLHours());
        section.setUserRecTTLMinutes(source.getUserRecTTLMinutes());
        return section;
    }

    private double toDouble(Object v) {
        return v instanceof Number n ? n.doubleValue() : Double.parseDouble(v.toString());
    }

    private int toInt(Object v) {
        return v instanceof Number n ? n.intValue() : Integer.parseInt(v.toString());
    }

    private int defaultInt(Integer value, int fallback) {
        return value == null ? fallback : value;
    }
}

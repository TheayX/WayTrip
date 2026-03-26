package com.travel.service.cache;

import com.travel.config.AppCacheProperties;
import com.travel.config.RedisKeyManager;
import com.travel.dto.recommendation.RecommendationAlgorithmConfigDTO;
import com.travel.dto.recommendation.RecommendationCacheConfigDTO;
import com.travel.dto.recommendation.RecommendationConfigBundleDTO;
import com.travel.dto.recommendation.RecommendationHeatConfigDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RecommendationCacheService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final AppCacheProperties appCacheProperties;

    public RecommendationConfigBundleDTO loadConfig() {
        RecommendationConfigBundleDTO mergedConfig = buildDefaultConfig();
        applyAlgorithmSection(
            mergedConfig,
            redisTemplate.opsForValue().get(RedisKeyManager.recommendationConfigAlgorithm())
        );
        applyHeatSection(
            mergedConfig,
            redisTemplate.opsForValue().get(RedisKeyManager.recommendationConfigHeat())
        );
        applyCacheSection(
            mergedConfig,
            redisTemplate.opsForValue().get(RedisKeyManager.recommendationConfigCache())
        );
        return mergedConfig;
    }

    public void saveConfig(RecommendationConfigBundleDTO config) {
        RecommendationConfigBundleDTO safeConfig = mergeBundle(buildDefaultConfig(), config);
        redisTemplate.opsForValue().set(RedisKeyManager.recommendationConfigAlgorithm(), safeAlgorithmConfig(safeConfig));
        redisTemplate.opsForValue().set(RedisKeyManager.recommendationConfigHeat(), safeHeatConfig(safeConfig));
        redisTemplate.opsForValue().set(RedisKeyManager.recommendationConfigCache(), safeCacheConfig(safeConfig));
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

    private RecommendationConfigBundleDTO buildDefaultConfig() {
        RecommendationConfigBundleDTO config = RecommendationConfigBundleDTO.defaultConfig();
        config.getCache().setUserRecTTLMinutes(defaultInt(appCacheProperties.getRecommendation().getUserRecTtlMinutes(), 60));
        config.getCache().setSimilarityTTLHours(defaultInt(appCacheProperties.getRecommendation().getSimilarityTtlHours(), 24));
        config.getHeat().setHeatViewDedupeWindowMinutes(defaultInt(appCacheProperties.getSpot().getHeatViewDedupeWindowMinutes(), 30));
        return config;
    }

    private boolean applyAlgorithmSection(RecommendationConfigBundleDTO target, Object cached) {
        RecommendationAlgorithmConfigDTO section = mapAlgorithmSection(cached);
        if (section == null) {
            return false;
        }
        mergeAlgorithm(target.getAlgorithm(), section);
        return true;
    }

    private boolean applyHeatSection(RecommendationConfigBundleDTO target, Object cached) {
        RecommendationHeatConfigDTO section = mapHeatSection(cached);
        if (section == null) {
            return false;
        }
        mergeHeat(target.getHeat(), section);
        return true;
    }

    private boolean applyCacheSection(RecommendationConfigBundleDTO target, Object cached) {
        RecommendationCacheConfigDTO section = mapCacheSection(cached);
        if (section == null) {
            return false;
        }
        mergeCache(target.getCache(), section);
        return true;
    }

    @SuppressWarnings("unchecked")
    private RecommendationAlgorithmConfigDTO mapAlgorithmSection(Object cached) {
        if (cached == null) {
            return null;
        }
        if (cached instanceof RecommendationAlgorithmConfigDTO config) {
            return config;
        }
        if (cached instanceof RecommendationConfigBundleDTO bundle) {
            return bundle.getAlgorithm();
        }
        if (!(cached instanceof Map<?, ?>)) {
            return null;
        }

        try {
            Map<String, Object> map = (Map<String, Object>) cached;
            RecommendationAlgorithmConfigDTO config = new RecommendationAlgorithmConfigDTO();
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
            if (map.containsKey("minInteractionsForCF")) config.setMinInteractionsForCF(toInt(map.get("minInteractionsForCF")));
            if (map.containsKey("topKNeighbors")) config.setTopKNeighbors(toInt(map.get("topKNeighbors")));
            if (map.containsKey("candidateExpandFactor")) config.setCandidateExpandFactor(toInt(map.get("candidateExpandFactor")));
            if (map.containsKey("coldStartExpandFactor")) config.setColdStartExpandFactor(toInt(map.get("coldStartExpandFactor")));
            return config;
        } catch (Exception e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private RecommendationHeatConfigDTO mapHeatSection(Object cached) {
        if (cached == null) {
            return null;
        }
        if (cached instanceof RecommendationHeatConfigDTO config) {
            return config;
        }
        if (cached instanceof RecommendationConfigBundleDTO bundle) {
            return bundle.getHeat();
        }
        if (!(cached instanceof Map<?, ?>)) {
            return null;
        }

        try {
            Map<String, Object> map = (Map<String, Object>) cached;
            RecommendationHeatConfigDTO config = new RecommendationHeatConfigDTO();
            if (map.containsKey("heatViewIncrement")) config.setHeatViewIncrement(toInt(map.get("heatViewIncrement")));
            if (map.containsKey("heatFavoriteIncrement")) config.setHeatFavoriteIncrement(toInt(map.get("heatFavoriteIncrement")));
            if (map.containsKey("heatReviewIncrement")) config.setHeatReviewIncrement(toInt(map.get("heatReviewIncrement")));
            if (map.containsKey("heatOrderPaidIncrement")) config.setHeatOrderPaidIncrement(toInt(map.get("heatOrderPaidIncrement")));
            if (map.containsKey("heatOrderCompletedIncrement")) config.setHeatOrderCompletedIncrement(toInt(map.get("heatOrderCompletedIncrement")));
            if (map.containsKey("heatViewDedupeWindowMinutes")) config.setHeatViewDedupeWindowMinutes(toInt(map.get("heatViewDedupeWindowMinutes")));
            if (map.containsKey("heatRerankFactor")) config.setHeatRerankFactor(toDouble(map.get("heatRerankFactor")));
            return config;
        } catch (Exception e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private RecommendationCacheConfigDTO mapCacheSection(Object cached) {
        if (cached == null) {
            return null;
        }
        if (cached instanceof RecommendationCacheConfigDTO config) {
            return config;
        }
        if (cached instanceof RecommendationConfigBundleDTO bundle) {
            return bundle.getCache();
        }
        if (!(cached instanceof Map<?, ?>)) {
            return null;
        }

        try {
            Map<String, Object> map = (Map<String, Object>) cached;
            RecommendationCacheConfigDTO config = new RecommendationCacheConfigDTO();
            if (map.containsKey("similarityTTLHours")) config.setSimilarityTTLHours(toInt(map.get("similarityTTLHours")));
            if (map.containsKey("userRecTTLMinutes")) config.setUserRecTTLMinutes(toInt(map.get("userRecTTLMinutes")));
            return config;
        } catch (Exception e) {
            return null;
        }
    }

    private RecommendationConfigBundleDTO mergeBundle(RecommendationConfigBundleDTO base, RecommendationConfigBundleDTO override) {
        RecommendationConfigBundleDTO merged = base == null ? RecommendationConfigBundleDTO.defaultConfig() : base;
        if (override == null) {
            return merged;
        }
        mergeAlgorithm(merged.getAlgorithm(), override.getAlgorithm());
        mergeHeat(merged.getHeat(), override.getHeat());
        mergeCache(merged.getCache(), override.getCache());
        return merged;
    }

    private void mergeAlgorithm(RecommendationAlgorithmConfigDTO target, RecommendationAlgorithmConfigDTO source) {
        if (target == null || source == null) {
            return;
        }
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
        if (source.getMinInteractionsForCF() != null) target.setMinInteractionsForCF(source.getMinInteractionsForCF());
        if (source.getTopKNeighbors() != null) target.setTopKNeighbors(source.getTopKNeighbors());
        if (source.getCandidateExpandFactor() != null) target.setCandidateExpandFactor(source.getCandidateExpandFactor());
        if (source.getColdStartExpandFactor() != null) target.setColdStartExpandFactor(source.getColdStartExpandFactor());
    }

    private void mergeHeat(RecommendationHeatConfigDTO target, RecommendationHeatConfigDTO source) {
        if (target == null || source == null) {
            return;
        }
        if (source.getHeatViewIncrement() != null) target.setHeatViewIncrement(source.getHeatViewIncrement());
        if (source.getHeatFavoriteIncrement() != null) target.setHeatFavoriteIncrement(source.getHeatFavoriteIncrement());
        if (source.getHeatReviewIncrement() != null) target.setHeatReviewIncrement(source.getHeatReviewIncrement());
        if (source.getHeatOrderPaidIncrement() != null) target.setHeatOrderPaidIncrement(source.getHeatOrderPaidIncrement());
        if (source.getHeatOrderCompletedIncrement() != null) target.setHeatOrderCompletedIncrement(source.getHeatOrderCompletedIncrement());
        if (source.getHeatViewDedupeWindowMinutes() != null) target.setHeatViewDedupeWindowMinutes(source.getHeatViewDedupeWindowMinutes());
        if (source.getHeatRerankFactor() != null) target.setHeatRerankFactor(source.getHeatRerankFactor());
    }

    private void mergeCache(RecommendationCacheConfigDTO target, RecommendationCacheConfigDTO source) {
        if (target == null || source == null) {
            return;
        }
        if (source.getSimilarityTTLHours() != null) target.setSimilarityTTLHours(source.getSimilarityTTLHours());
        if (source.getUserRecTTLMinutes() != null) target.setUserRecTTLMinutes(source.getUserRecTTLMinutes());
    }

    private RecommendationAlgorithmConfigDTO safeAlgorithmConfig(RecommendationConfigBundleDTO config) {
        return config == null || config.getAlgorithm() == null
            ? new RecommendationAlgorithmConfigDTO()
            : config.getAlgorithm();
    }

    private RecommendationHeatConfigDTO safeHeatConfig(RecommendationConfigBundleDTO config) {
        return config == null || config.getHeat() == null
            ? new RecommendationHeatConfigDTO()
            : config.getHeat();
    }

    private RecommendationCacheConfigDTO safeCacheConfig(RecommendationConfigBundleDTO config) {
        return config == null || config.getCache() == null
            ? new RecommendationCacheConfigDTO()
            : config.getCache();
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

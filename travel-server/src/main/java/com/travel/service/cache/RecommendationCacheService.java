package com.travel.service.cache;

import com.travel.config.RedisKeyManager;
import com.travel.dto.recommendation.LegacyRecommendationConfigDTO;
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

    public Object getAlgorithmConfigSection() {
        return redisTemplate.opsForValue().get(RedisKeyManager.recommendationConfigAlgorithm());
    }

    public Object getHeatConfigSection() {
        return redisTemplate.opsForValue().get(RedisKeyManager.recommendationConfigHeat());
    }

    public Object getCacheConfigSection() {
        return redisTemplate.opsForValue().get(RedisKeyManager.recommendationConfigCache());
    }

    public Object getLegacyConfig() {
        return redisTemplate.opsForValue().get(RedisKeyManager.recommendationConfig());
    }

    public void saveConfigSections(
        LegacyRecommendationConfigDTO algorithmConfig,
        LegacyRecommendationConfigDTO heatConfig,
        LegacyRecommendationConfigDTO cacheConfig,
        LegacyRecommendationConfigDTO legacyConfig
    ) {
        redisTemplate.opsForValue().set(RedisKeyManager.recommendationConfigAlgorithm(), algorithmConfig);
        redisTemplate.opsForValue().set(RedisKeyManager.recommendationConfigHeat(), heatConfig);
        redisTemplate.opsForValue().set(RedisKeyManager.recommendationConfigCache(), cacheConfig);
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
}

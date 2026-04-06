package com.travel.service.cache;

import com.travel.config.cache.AppCacheProperties;
import com.travel.config.cache.RedisKeyManager;
import com.travel.dto.banner.response.BannerResponse;
import com.travel.dto.home.response.HotSpotResponse;
import com.travel.dto.recommendation.config.RecommendationAlgorithmConfigDTO;
import com.travel.dto.recommendation.config.RecommendationCacheConfigDTO;
import com.travel.dto.recommendation.config.RecommendationConfigBundleDTO;
import com.travel.dto.recommendation.config.RecommendationHeatConfigDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 推荐缓存服务。
 * <p>
 * 负责管理推荐配置缓存、用户推荐结果缓存、景点相似度缓存以及推荐引擎状态缓存。
 */
@Service
@RequiredArgsConstructor
public class RecommendationCacheService {

    // Redis 访问与默认配置依赖
    private final RedisTemplate<String, Object> redisTemplate;
    private final AppCacheProperties appCacheProperties;

    /**
     * 从 Redis 加载推荐配置，并与默认配置合并。
     *
     * @return 合并后的完整推荐配置
     */
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

    /**
     * 保存推荐配置到 Redis。
     *
     * @param config 要保存的推荐配置；保存前会与默认配置合并
     */
    public void saveConfig(RecommendationConfigBundleDTO config) {
        RecommendationConfigBundleDTO safeConfig = mergeBundle(buildDefaultConfig(), config);
        redisTemplate.opsForValue().set(RedisKeyManager.recommendationConfigAlgorithm(), safeAlgorithmConfig(safeConfig));
        redisTemplate.opsForValue().set(RedisKeyManager.recommendationConfigHeat(), safeHeatConfig(safeConfig));
        redisTemplate.opsForValue().set(RedisKeyManager.recommendationConfigCache(), safeCacheConfig(safeConfig));
    }

    /**
     * 获取指定用户的推荐结果缓存。
     *
     * @param userId 用户 ID
     * @return 推荐结果缓存对象；通常为 {@code Map<Long, Double>}
     */
    public Object getUserRecommendation(Long userId) {
        return redisTemplate.opsForValue().get(RedisKeyManager.recommendationUser(userId));
    }

    /**
     * 保存用户推荐结果到 Redis。
     *
     * @param userId 用户 ID
     * @param scores 推荐分数映射（景点 ID -> 分数）
     * @param ttlMinutes 过期时间（分钟）
     */
    public void saveUserRecommendation(Long userId, Map<Long, Double> scores, long ttlMinutes) {
        redisTemplate.opsForValue().set(
            RedisKeyManager.recommendationUser(userId),
            scores,
            ttlMinutes,
            TimeUnit.MINUTES
        );
    }

    /**
     * 删除用户推荐结果缓存。
     *
     * @param userId 用户 ID
     */
    public void deleteUserRecommendation(Long userId) {
        redisTemplate.delete(RedisKeyManager.recommendationUser(userId));
    }

    /**
     * 获取指定景点的相似度缓存。
     *
     * @param spotId 景点 ID
     * @return 相似度缓存对象；通常为 {@code Map<Long, Double>}
     */
    public Object getSimilarity(Long spotId) {
        return redisTemplate.opsForValue().get(RedisKeyManager.recommendationSimilarity(spotId));
    }

    /**
     * 保存景点相似度数据到 Redis。
     *
     * @param spotId 景点 ID
     * @param similarities 相似度映射（其他景点 ID -> 相似度分数）
     * @param ttlHours 过期时间（小时）
     */
    public void saveSimilarity(Long spotId, Map<Long, Double> similarities, long ttlHours) {
        redisTemplate.opsForValue().set(
            RedisKeyManager.recommendationSimilarity(spotId),
            similarities,
            ttlHours,
            TimeUnit.HOURS
        );
    }

    /**
     * 获取推荐系统运行状态缓存。
     *
     * @return 状态缓存对象
     */
    public Object getStatus() {
        return redisTemplate.opsForValue().get(RedisKeyManager.recommendationStatus());
    }

    /**
     * 保存推荐系统运行状态到 Redis。
     *
     * @param statusMap 状态信息映射
     */
    public void saveStatus(Map<String, Object> statusMap) {
        redisTemplate.opsForValue().set(RedisKeyManager.recommendationStatus(), statusMap);
    }

    /**
     * 获取首页热门景点缓存。
     *
     * @param limit 请求条数
     * @return 首页热门景点响应；缓存不存在或类型不匹配时返回 null
     */
    public HotSpotResponse getHomeHotSpots(Integer limit) {
        Object cached = redisTemplate.opsForValue().get(RedisKeyManager.homeHotSpots(limit));
        return cached instanceof HotSpotResponse response ? response : null;
    }

    /**
     * 保存首页热门景点缓存。
     *
     * @param limit 请求条数
     * @param response 热门景点响应
     */
    public void saveHomeHotSpots(Integer limit, HotSpotResponse response) {
        redisTemplate.opsForValue().set(
            RedisKeyManager.homeHotSpots(limit),
            response,
            defaultInt(appCacheProperties.getHome().getHotSpotsTtlMinutes(), 10),
            TimeUnit.MINUTES
        );
    }

    /**
     * 清理首页热门景点缓存（按不同 limit 全量失效）。
     */
    public void deleteHomeHotSpots() {
        Set<String> keys = redisTemplate.keys("waytrip:home:hot:*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    /**
     * 获取首页轮播图缓存。
     *
     * @return 轮播图响应；缓存不存在或类型不匹配时返回 null
     */
    public BannerResponse getHomeBanners() {
        Object cached = redisTemplate.opsForValue().get(RedisKeyManager.homeBanners());
        return cached instanceof BannerResponse response ? response : null;
    }

    /**
     * 保存首页轮播图缓存。
     *
     * @param response 轮播图响应
     */
    public void saveHomeBanners(BannerResponse response) {
        redisTemplate.opsForValue().set(
            RedisKeyManager.homeBanners(),
            response,
            defaultInt(appCacheProperties.getHome().getBannersTtlMinutes(), 10),
            TimeUnit.MINUTES
        );
    }

    /**
     * 清理首页轮播图缓存。
     */
    public void deleteHomeBanners() {
        redisTemplate.delete(RedisKeyManager.homeBanners());
    }

    /**
     * 构建默认推荐配置。
     *
     * @return 默认配置对象
     */
    private RecommendationConfigBundleDTO buildDefaultConfig() {
        RecommendationConfigBundleDTO config = RecommendationConfigBundleDTO.defaultConfig();
        config.getCache().setUserRecTTLMinutes(defaultInt(appCacheProperties.getRecommendation().getUserRecTtlMinutes(), 60));
        config.getCache().setSimilarityTTLHours(defaultInt(appCacheProperties.getRecommendation().getSimilarityTtlHours(), 24));
        return config;
    }

    /**
     * 将算法配置缓存合并到目标配置中。
     *
     * @param target 目标配置
     * @param cached 缓存中的配置
     * @return 是否成功应用
     */
    private boolean applyAlgorithmSection(RecommendationConfigBundleDTO target, Object cached) {
        RecommendationAlgorithmConfigDTO section = mapAlgorithmSection(cached);
        if (section == null) {
            return false;
        }
        mergeAlgorithm(target.getAlgorithm(), section);
        return true;
    }

    /**
     * 将热度配置缓存合并到目标配置中。
     *
     * @param target 目标配置
     * @param cached 缓存中的配置
     * @return 是否成功应用
     */
    private boolean applyHeatSection(RecommendationConfigBundleDTO target, Object cached) {
        RecommendationHeatConfigDTO section = mapHeatSection(cached);
        if (section == null) {
            return false;
        }
        mergeHeat(target.getHeat(), section);
        return true;
    }

    /**
     * 将缓存配置合并到目标配置中。
     *
     * @param target 目标配置
     * @param cached 缓存中的配置
     * @return 是否成功应用
     */
    private boolean applyCacheSection(RecommendationConfigBundleDTO target, Object cached) {
        RecommendationCacheConfigDTO section = mapCacheSection(cached);
        if (section == null) {
            return false;
        }
        mergeCache(target.getCache(), section);
        return true;
    }

    /**
     * 将缓存对象转换为算法配置对象。
     *
     * @param cached 缓存中的数据
     * @return 算法配置对象，转换失败返回 null
     */
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
            if (map.containsKey("viewSourceFactorRecommendation")) config.setViewSourceFactorRecommendation(toDouble(map.get("viewSourceFactorRecommendation")));
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

    /**
     * 将缓存对象转换为热度配置对象。
     *
     * @param cached 缓存中的数据
     * @return 热度配置对象，转换失败返回 null
     */
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
            if (map.containsKey("heatRerankFactor")) config.setHeatRerankFactor(toDouble(map.get("heatRerankFactor")));
            return config;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 将缓存对象转换为缓存配置对象。
     *
     * @param cached 缓存中的数据
     * @return 缓存配置对象，转换失败返回 null
     */
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

    /**
     * 合并两个配置包。
     *
     * @param base 基础配置
     * @param override 覆盖配置
     * @return 合并后的配置
     */
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

    /**
     * 合并算法配置，使用源配置中的非空值覆盖目标配置。
     *
     * @param target 目标配置
     * @param source 源配置
     */
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
        if (source.getViewSourceFactorRecommendation() != null) target.setViewSourceFactorRecommendation(source.getViewSourceFactorRecommendation());
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

    /**
     * 合并热度配置，使用源配置中的非空值覆盖目标配置。
     *
     * @param target 目标配置
     * @param source 源配置
     */
    private void mergeHeat(RecommendationHeatConfigDTO target, RecommendationHeatConfigDTO source) {
        if (target == null || source == null) {
            return;
        }
        if (source.getHeatViewIncrement() != null) target.setHeatViewIncrement(source.getHeatViewIncrement());
        if (source.getHeatFavoriteIncrement() != null) target.setHeatFavoriteIncrement(source.getHeatFavoriteIncrement());
        if (source.getHeatReviewIncrement() != null) target.setHeatReviewIncrement(source.getHeatReviewIncrement());
        if (source.getHeatOrderPaidIncrement() != null) target.setHeatOrderPaidIncrement(source.getHeatOrderPaidIncrement());
        if (source.getHeatOrderCompletedIncrement() != null) target.setHeatOrderCompletedIncrement(source.getHeatOrderCompletedIncrement());
        if (source.getHeatRerankFactor() != null) target.setHeatRerankFactor(source.getHeatRerankFactor());
    }

    /**
     * 合并缓存配置，使用源配置中的非空值覆盖目标配置。
     *
     * @param target 目标配置
     * @param source 源配置
     */
    private void mergeCache(RecommendationCacheConfigDTO target, RecommendationCacheConfigDTO source) {
        if (target == null || source == null) {
            return;
        }
        if (source.getSimilarityTTLHours() != null) target.setSimilarityTTLHours(source.getSimilarityTTLHours());
        if (source.getUserRecTTLMinutes() != null) target.setUserRecTTLMinutes(source.getUserRecTTLMinutes());
    }

    /**
     * 安全获取算法配置；为空时返回新实例。
     *
     * @param config 推荐配置包
     * @return 算法配置对象
     */
    private RecommendationAlgorithmConfigDTO safeAlgorithmConfig(RecommendationConfigBundleDTO config) {
        return config == null || config.getAlgorithm() == null
            ? new RecommendationAlgorithmConfigDTO()
            : config.getAlgorithm();
    }

    /**
     * 安全获取热度配置；为空时返回新实例。
     *
     * @param config 推荐配置包
     * @return 热度配置对象
     */
    private RecommendationHeatConfigDTO safeHeatConfig(RecommendationConfigBundleDTO config) {
        return config == null || config.getHeat() == null
            ? new RecommendationHeatConfigDTO()
            : config.getHeat();
    }

    /**
     * 安全获取缓存配置；为空时返回新实例。
     *
     * @param config 推荐配置包
     * @return 缓存配置对象
     */
    private RecommendationCacheConfigDTO safeCacheConfig(RecommendationConfigBundleDTO config) {
        return config == null || config.getCache() == null
            ? new RecommendationCacheConfigDTO()
            : config.getCache();
    }

    /**
     * 将对象转换为 double 值。
     *
     * @param v 原始对象
     * @return double 值
     */
    private double toDouble(Object v) {
        return v instanceof Number n ? n.doubleValue() : Double.parseDouble(v.toString());
    }

    /**
     * 将对象转换为 int 值。
     *
     * @param v 原始对象
     * @return int 值
     */
    private int toInt(Object v) {
        return v instanceof Number n ? n.intValue() : Integer.parseInt(v.toString());
    }

    /**
     * 获取整数配置值，为空时返回默认值。
     *
     * @param value 原始整数值
     * @param fallback 默认值
     * @return 有效整数值
     */
    private int defaultInt(Integer value, int fallback) {
        return value == null ? fallback : value;
    }
}

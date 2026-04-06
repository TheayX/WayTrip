package com.travel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.travel.common.exception.BusinessException;
import com.travel.common.result.ResultCode;
import com.travel.dto.home.response.HotSpotResponse;
import com.travel.dto.home.response.NearbySpotResponse;
import com.travel.dto.home.item.RecentViewedSpotItem;
import com.travel.dto.home.response.RecentViewedSpotResponse;
import com.travel.dto.recommendation.config.RecommendationAlgorithmConfigDTO;
import com.travel.dto.recommendation.config.RecommendationCacheConfigDTO;
import com.travel.dto.recommendation.config.RecommendationConfigBundleDTO;
import com.travel.dto.recommendation.config.RecommendationHeatConfigDTO;
import com.travel.dto.recommendation.response.RecommendationResponse;
import com.travel.dto.recommendation.response.RecommendationStatusDTO;
import com.travel.dto.recommendation.response.SimilarityPreviewResponse;
import com.travel.entity.*;
import com.travel.mapper.*;
import com.travel.service.cache.RecommendationCacheService;
import com.travel.service.RecommendationService;
import com.travel.service.support.recommendation.RecommendationConfigSupport;
import com.travel.service.support.recommendation.RecommendationColdStartSupport;
import com.travel.service.support.recommendation.RecommendationQuerySupport;
import com.travel.service.support.recommendation.RecommendationScoreSupport;
import com.travel.service.support.recommendation.RecommendationSimilaritySupport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * 推荐服务实现，负责个性化推荐、冷启动兜底、相似度矩阵维护与调试能力。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationServiceImpl implements RecommendationService {

    // 持久层与缓存依赖

    private final SpotMapper spotMapper;
    private final ReviewMapper reviewMapper;
    private final UserSpotFavoriteMapper userSpotFavoriteMapper;
    private final OrderMapper orderMapper;
    private final UserSpotViewMapper userSpotViewMapper;
    private final SpotCategoryMapper categoryMapper;
    private final SpotRegionMapper spotRegionMapper;
    private final UserPreferenceMapper userPreferenceMapper;
    private final RecommendationCacheService recommendationCacheService;
    private final RecommendationQuerySupport recommendationQuerySupport;
    private final RecommendationConfigSupport recommendationConfigSupport;
    private final RecommendationSimilaritySupport recommendationSimilaritySupport;
    private final RecommendationScoreSupport recommendationScoreSupport;
    private final RecommendationColdStartSupport recommendationColdStartSupport;

    private final AtomicBoolean computing = new AtomicBoolean(false);

    // 推荐主链路入口

    @Override
    public RecommendationResponse getRecommendations(Long userId, Integer limit) {
        if (limit == null || limit <= 0) limit = 10;

        // 先查用户推荐缓存。
        Object cached = recommendationCacheService.getUserRecommendation(userId);

        if (cached instanceof Map<?, ?> cachedMap && !cachedMap.isEmpty()) {
            Map<Long, Double> cachedScores = castScoreMap(cachedMap);
            if (!cachedScores.isEmpty()) {
                return buildRecommendationResponse(new ArrayList<>(cachedScores.keySet()), cachedScores, limit, "personalized", false);
            }
        }

        if (cached instanceof List<?> cachedIds && !cachedIds.isEmpty()) {
            List<Long> recommendationIds = cachedIds.stream()
                .map(this::castToLong)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
            if (!recommendationIds.isEmpty()) {
                return buildRecommendationResponse(recommendationIds, null, limit, "personalized", false);
            }
        }

        return computeRecommendations(userId, limit, false, false);
    }

    @Override
    public RecommendationResponse refreshRecommendations(Long userId, Integer limit) {
        if (limit == null || limit <= 0) limit = 10;
        
        // 刷新前先清理缓存。
        recommendationCacheService.deleteUserRecommendation(userId);
        
        return computeRecommendations(userId, limit, true, false);
    }

    @Override
    public RecommendationResponse previewRecommendations(Long userId, Integer limit, Boolean refresh, Boolean debug, Boolean stable) {
        if (limit == null || limit <= 0) limit = 10;
        boolean refreshMode = Boolean.TRUE.equals(refresh);
        boolean debugMode = Boolean.TRUE.equals(debug);
        boolean stableMode = Boolean.TRUE.equals(stable);
        RecommendationResponse response = debugMode
            ? computeRecommendations(userId, limit, refreshMode, true, stableMode)
            : (refreshMode
                ? computeRecommendations(userId, limit, true, false, stableMode)
                : getRecommendations(userId, limit));
        if (Boolean.TRUE.equals(debug)) {
            logRecommendationPreview(userId, response, refreshMode);
        }
        return response;
    }

    @Override
    public void invalidateUserRecommendationCache(Long userId) {
        if (userId == null) {
            return;
        }
        recommendationCacheService.deleteUserRecommendation(userId);
    }

    // 推荐结果计算与冷启动兜底

    private RecommendationResponse computeRecommendations(Long userId, Integer limit) {
        return computeRecommendations(userId, limit, false, false, false);
    }

    private RecommendationResponse computeRecommendations(Long userId, Integer limit, boolean refresh) {
        return computeRecommendations(userId, limit, refresh, false, false);
    }

    private RecommendationResponse computeRecommendations(Long userId, Integer limit, boolean refresh, boolean debug) {
        return computeRecommendations(userId, limit, refresh, debug, false);
    }

    private RecommendationResponse computeRecommendations(Long userId, Integer limit, boolean refresh, boolean debug, boolean stable) {
        RecommendationConfigBundleDTO config = recommendationCacheService.loadConfig();
        RecommendationAlgorithmConfigDTO algorithmConfig = safeAlgorithmConfig(config);
        RecommendationHeatConfigDTO heatConfig = safeHeatConfig(config);
        RecommendationCacheConfigDTO cacheConfig = safeCacheConfig(config);
        RecommendationResponse.DebugInfo debugInfo = debug ? initDebugInfo(userId, limit, refresh) : null;

        log.info(
            "开始计算推荐结果：用户ID={}，请求数量={}，是否刷新={}，是否调试={}，协同过滤最少交互数={}，候选扩容倍数={}",
            userId,
            limit,
            refresh,
            debug,
            defaultInt(algorithmConfig.getMinInteractionsForCF(), 3),
            getCandidateExpandFactor(algorithmConfig)
        );

        // ============ 根据总交互数判断是否走冷启动 ============
        Map<Long, Double> userInteractions = buildUserInteractionWeights(userId, algorithmConfig);
        populateBehaviorStats(debugInfo, userId);
        populateBehaviorDetails(debugInfo, userId, algorithmConfig);
        populateInteractionDebugInfo(debugInfo, userInteractions);
        logUserInteractionWeights(userId, userInteractions, debug);

        if (userInteractions.size() < defaultInt(algorithmConfig.getMinInteractionsForCF(), 3)) {
            log.info(
                "降级为冷启动：用户ID={}，交互景点数={}，阈值={}，原因=交互不足",
                userId,
                userInteractions.size(),
                defaultInt(algorithmConfig.getMinInteractionsForCF(), 3)
            );
            if (debugInfo != null) {
                debugInfo.setTriggerReason("交互数量不足，切换为冷启动");
                debugInfo.setNotes(List.of(
                    "用户交互景点数低于协同过滤阈值。",
                    "请检查用户历史行为是否足够，或适当调低 minInteractionsForCF。"
                ));
            }
            return handleColdStart(userId, limit, refresh, debug, stable, debugInfo);
        }

        // 基于 ItemCF 计算个性化推荐。
        int candidateLimit = limit * getCandidateExpandFactor(algorithmConfig);
        Map<Long, List<RecommendationResponse.DebugEntry>> contributionMap = debug ? new HashMap<>() : null;
        Map<Long, Double> recommendedScores = computeItemCFRecommendationScores(userInteractions, candidateLimit, contributionMap);
        populateScoreDebugEntries(debugInfo, recommendedScores, "ItemCF 原始候选分数");
        logScoreMap("ItemCF 原始候选分数", recommendedScores, debug);
        List<Long> recommendedIds = new ArrayList<>(recommendedScores.keySet());
        
        // 过滤用户已经交互过的景点。
        List<Long> filteredIds = filterInteractedSpots(userId, recommendedIds);
        populateFilteredOutDebugEntries(debugInfo, recommendedIds, filteredIds, "候选景点已与当前用户发生过交互，已被过滤");
        logFilteredRecommendations(userId, recommendedIds, filteredIds, debug);
        Map<Long, Double> filteredScores = orderScoresByIds(filteredIds, recommendedScores);
        populateFilteredScoresDebugEntries(debugInfo, filteredScores, "过滤后的候选分数");
        logScoreMap("过滤后的候选分数", filteredScores, debug);
        filteredScores = applyHeatRerank(filteredScores, heatConfig, debug);
        populateRerankedScoreDebugEntries(debugInfo, filteredScores, "热度重排后的候选分数");
        filteredIds = new ArrayList<>(filteredScores.keySet());
        
        if (filteredIds.isEmpty()) {
            log.info("个性化推荐结果过滤后为空，降级为冷启动：用户ID={}", userId);
            if (debugInfo != null) {
                debugInfo.setTriggerReason("协同过滤候选集在过滤后为空，降级为冷启动");
            }
            return handleColdStart(userId, limit, refresh, debug, stable, debugInfo);
        }

        // 缓存推荐分数。
        if (refresh && !stable) {
            filteredIds = rotateRecommendations(filteredIds, limit);
            filteredScores = orderScoresByIds(filteredIds, filteredScores);
        }

        recommendationCacheService.saveUserRecommendation(
            userId,
            filteredScores,
            defaultInt(cacheConfig.getUserRecTTLMinutes(), 60)
        );

        log.info(
            "推荐结果计算完成：用户ID={}，推荐类型=personalized，候选数={}，最终返回数={}，缓存时长={}分钟",
            userId,
            recommendedScores.size(),
            Math.min(filteredIds.size(), limit),
            defaultInt(cacheConfig.getUserRecTTLMinutes(), 60)
        );

        if (debugInfo != null) {
            debugInfo.setTriggerReason("命中协同过滤主链路");
            debugInfo.setResultContributions(buildResultContributions(filteredScores, contributionMap));
            debugInfo.setNotes(List.of(
                "当前结果来自个性化协同过滤链路。",
                "可重点关注交互权重、候选分数、结果贡献来源和热度重排变化。"
            ));
        }
        return buildRecommendationResponse(filteredIds, filteredScores, limit, "personalized", false, debugInfo);
    }

    // 用户行为权重构建

    /**
     * 构建单个用户的融合交互权重：Map<spotId, weight>。
     */
    private Map<Long, Double> buildUserInteractionWeights(Long userId, RecommendationAlgorithmConfigDTO config) {
        return recommendationScoreSupport.buildUserInteractionWeights(userId, config);
    }


    // 冷启动推荐与兜底补齐

    private RecommendationResponse handleColdStart(Long userId, Integer limit, boolean refresh, boolean debug,
                                                   boolean stable,
                                                   RecommendationResponse.DebugInfo debugInfo) {
        RecommendationAlgorithmConfigDTO algorithmConfig = safeAlgorithmConfig(recommendationCacheService.loadConfig());
        HotSpotResponse hotSpots = getHotSpots(refresh ? Math.max(limit * getColdStartExpandFactor(algorithmConfig), limit) : limit);
        return recommendationColdStartSupport.handleColdStart(
            userId,
            limit,
            refresh,
            debug,
            stable,
            debugInfo,
            algorithmConfig,
            hotSpots,
            ignored -> getColdStartExpandFactor(algorithmConfig),
            this::rotateRecommendations,
            this::rotateSpotItems,
            spotIds -> buildRecommendationResponse(spotIds, limit, "preference", false, debugInfo),
            hotSpotList -> {
                RecommendationResponse response = new RecommendationResponse();
                response.setType("hot");
                response.setNeedPreference(true);
                response.setDebugInfo(debugInfo);
                response.setList(hotSpotList.stream()
                    .limit(limit)
                    .map(item -> {
                        RecommendationResponse.SpotItem spotItem = new RecommendationResponse.SpotItem();
                        spotItem.setId(item.getId());
                        spotItem.setName(item.getName());
                        spotItem.setCoverImage(item.getCoverImage());
                        spotItem.setPrice(item.getPrice());
                        spotItem.setAvgRating(item.getAvgRating());
                        spotItem.setCategoryName(item.getCategoryName());
                        return spotItem;
                    })
                    .collect(Collectors.toList()));
                return response;
            },
            context -> logColdStartResult(context.userId(), context.type(), context.categoryIds(), context.spotIds(), context.debug())
        );
    }

    private List<Long> getHotFallbackSpotIds(Collection<Long> excludedSpotIds, int limit) {
        return recommendationColdStartSupport.getHotFallbackSpotIds(excludedSpotIds, limit);
    }

    private List<Long> getUserPreferenceCategoryIds(Long userId) {
        return recommendationColdStartSupport.getUserPreferenceCategoryIds(userId);
    }

    private Long parsePreferenceCategoryId(String tag) {
        return recommendationColdStartSupport.parsePreferenceCategoryId(tag);
    }

    /**
     * 刷新推荐时轮换结果，避免每次都看到完全相同的一组。
     */
    private List<Long> rotateRecommendations(List<Long> recommendationIds, Integer limit) {
        if (recommendationIds == null || recommendationIds.size() <= 1) {
            return recommendationIds;
        }

        List<Long> rotatedIds = new ArrayList<>(recommendationIds);
        int rotationBase = Math.min(limit, rotatedIds.size() - 1);
        if (rotationBase <= 0) {
            return rotatedIds;
        }

        int offset = 1 + Math.abs(Objects.hash(System.nanoTime(), recommendationIds.get(0))) % rotationBase;
        Collections.rotate(rotatedIds, -offset);
        return rotatedIds;
    }

    /**
     * 刷新热门/偏好冷启动结果时轮换列表。
     */
    private void rotateSpotItems(List<HotSpotResponse.SpotItem> spotItems, Integer limit) {
        if (spotItems == null || spotItems.size() <= 1) {
            return;
        }

        int rotationBase = Math.min(limit, spotItems.size() - 1);
        if (rotationBase <= 0) {
            return;
        }

        int offset = 1 + Math.abs(Objects.hash(System.nanoTime(), spotItems.get(0).getId())) % rotationBase;
        Collections.rotate(spotItems, -offset);
    }

    private Map<Long, Double> computeItemCFRecommendationScores(Map<Long, Double> userInteractions, Integer limit) {
        return computeItemCFRecommendationScores(userInteractions, limit, null);
    }

    private Map<Long, Double> computeItemCFRecommendationScores(Map<Long, Double> userInteractions, Integer limit,
                                                                Map<Long, List<RecommendationResponse.DebugEntry>> contributionMap) {
        if (userInteractions.isEmpty()) {
            return Collections.emptyMap();
        }

        // 计算推荐分数。
        Map<Long, Double> scores = new HashMap<>();
        
        for (Map.Entry<Long, Double> entry : userInteractions.entrySet()) {
            Long spotId = entry.getKey();
            Double rui = entry.getValue(); // 融合后的交互权重

            // 读取 Top-K 相似景点。
            Map<Long, Double> similarities = recommendationSimilaritySupport.getSimilarSpots(spotId);
            
            for (Map.Entry<Long, Double> simEntry : similarities.entrySet()) {
                Long similarSpotId = simEntry.getKey();
                Double wji = simEntry.getValue(); // 相似度分数

                // 跳过用户已经交互过的景点。
                if (userInteractions.containsKey(similarSpotId)) {
                    continue;
                }
                
                // 公式：P_uj += w_ji * r_ui
                double contribution = wji * rui;
                scores.merge(similarSpotId, contribution, Double::sum);
                if (contributionMap != null) {
                    contributionMap.computeIfAbsent(similarSpotId, key -> new ArrayList<>())
                        .add(new RecommendationResponse.DebugEntry(
                            spotId,
                            getSpotName(spotId),
                            contribution,
                            String.format(Locale.ROOT, "由该历史景点贡献：相似度=%.4f，交互权重=%.4f", wji, rui)
                        ));
                }
            }
        }

        // 按分数降序返回。
        return scores.entrySet().stream()
            .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
            .limit(limit)
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                (left, right) -> left,
                LinkedHashMap::new
            ));
    }

    private Map<Long, Double> applyHeatRerank(Map<Long, Double> scoreMap, RecommendationHeatConfigDTO config) {
        return applyHeatRerank(scoreMap, config, false);
    }

    private Map<Long, Double> applyHeatRerank(Map<Long, Double> scoreMap, RecommendationHeatConfigDTO config, boolean debug) {
        return recommendationScoreSupport.applyHeatRerank(scoreMap, config, debug);
    }

    private Long castToLong(Object value) {
        return recommendationScoreSupport.castToLong(value);
    }

    private Double castToDouble(Object value) {
        return recommendationScoreSupport.castToDouble(value);
    }

    /**
     * 过滤用户已经交互过的景点。
     */
    private List<Long> filterInteractedSpots(Long userId, List<Long> spotIds) {
        return recommendationScoreSupport.filterInteractedSpots(userId, spotIds);
    }


    // 热门与附近景点能力

    @Override
    public HotSpotResponse getHotSpots(Integer limit) {
        if (limit == null || limit <= 0) limit = 10;

        HotSpotResponse cachedResponse = recommendationCacheService.getHomeHotSpots(limit);
        if (cachedResponse != null && cachedResponse.getList() != null) {
            return cachedResponse;
        }

        List<Spot> spots = spotMapper.selectList(
            new LambdaQueryWrapper<Spot>()
                .eq(Spot::getIsPublished, 1)
                .eq(Spot::getIsDeleted, 0)
                .orderByDesc(Spot::getHeatScore)
                .last("LIMIT " + limit)
        );

        // 获取分类名称
        Map<Long, String> categoryMap = getCategoryMap();

        HotSpotResponse response = new HotSpotResponse();
        response.setList(spots.stream().map(spot -> {
            HotSpotResponse.SpotItem item = new HotSpotResponse.SpotItem();
            item.setId(spot.getId());
            item.setName(spot.getName());
            item.setCoverImage(spot.getCoverImageUrl());
            item.setPrice(spot.getPrice());
            item.setAvgRating(spot.getAvgRating());
            item.setHeatScore(spot.getHeatScore());
            item.setCategoryName(categoryMap.get(spot.getCategoryId()));
            return item;
        }).collect(Collectors.toList()));

        recommendationCacheService.saveHomeHotSpots(limit, response);

        return response;
    }

    @Override
    public RecentViewedSpotResponse getRecentViewedSpots(Integer days, Integer limit) {
        int safeDays = days == null || days <= 0 ? 14 : days;
        int safeLimit = limit == null || limit <= 0 ? 12 : limit;

        List<RecentViewedSpotItem> list = userSpotViewMapper.selectRecentViewedSpots(
            LocalDateTime.now().minusDays(safeDays),
            safeLimit
        );

        RecentViewedSpotResponse response = new RecentViewedSpotResponse();
        response.setList(list);
        response.setDays(safeDays);
        return response;
    }

    @Override
    public NearbySpotResponse getNearbySpots(BigDecimal latitude, BigDecimal longitude, Integer limit) {
        if (latitude == null || longitude == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "经纬度不能为空");
        }
        if (limit == null || limit <= 0) limit = 3;

        List<NearbySpotResponse.SpotItem> list = spotMapper.selectNearbySpots(latitude, longitude, limit);
        list.forEach(item -> {
            if (item.getDistanceKm() != null) {
                item.setDistanceKm(item.getDistanceKm().setScale(1, RoundingMode.HALF_UP));
            }
        });

        NearbySpotResponse response = new NearbySpotResponse();
        response.setList(list);
        response.setTotal(list.size());
        response.setNearestDistanceKm(list.isEmpty() ? null : list.get(0).getDistanceKm());
        return response;
    }

    // 相似度矩阵离线更新

    /**
     * 离线重建景点相似度矩阵。
     *
     * 1. 将浏览、收藏、评分和订单行为合并为交互矩阵。
     * 2. 计算 IUF 加权余弦相似度，并把 Top-K 邻居缓存到 Redis。
     */
    @Override
    public void updateSimilarityMatrix() {
        if (!computing.compareAndSet(false, true)) {
            log.warn("相似度矩阵正在更新中，跳过重复请求");
            return;
        }

        try {
            RecommendationConfigBundleDTO config = recommendationCacheService.loadConfig();
            RecommendationAlgorithmConfigDTO algorithmConfig = safeAlgorithmConfig(config);
            RecommendationCacheConfigDTO cacheConfig = safeCacheConfig(config);
            log.info("开始更新相似度矩阵");
            Set<Long> activeSpotIds = recommendationSimilaritySupport.getActiveSpotIds();
            if (activeSpotIds.isEmpty()) {
                log.info("没有有效上架景点，跳过相似度矩阵更新");
                return;
            }

            RecommendationSimilaritySupport.OfflineMatrixSnapshot snapshot =
                recommendationSimilaritySupport.buildOfflineInteractionMatrix(activeSpotIds, algorithmConfig);
            Map<Long, Map<Long, Double>> userItemMatrix = snapshot.userItemMatrix();
            Set<Long> allSpotIds = snapshot.allSpotIds();

            if (userItemMatrix.isEmpty()) {
                log.info("没有交互数据，跳过相似度矩阵更新");
                return;
            }

            log.info("交互矩阵构建完成：用户数={}，景点数={}", userItemMatrix.size(), allSpotIds.size());

            // ============ 第 2 步：预计算 IUF 所需的 |N(u)| ============
            Map<Long, Integer> userActivityCount = recommendationSimilaritySupport.summarizeUserActivityCount(userItemMatrix);

            // ============ 第 3 步：构建景点到用户的倒排索引 ============
            // Map<spotId, Set<userId>> 表示 N(i)
            Map<Long, Set<Long>> spotUserSets = recommendationSimilaritySupport.buildSpotUserIndex(userItemMatrix);

            // ============ 第 4 步：计算 IUF 加权相似度 ============
            int topK = defaultInt(algorithmConfig.getTopKNeighbors(), 20);
            int simTTL = defaultInt(cacheConfig.getSimilarityTTLHours(), 24);
            recommendationSimilaritySupport.cacheSimilarityNeighbors(
                allSpotIds,
                spotUserSets,
                userActivityCount,
                algorithmConfig,
                cacheConfig
            );

            // ============ 保存状态摘要 ============
            recommendationSimilaritySupport.saveOfflineSummary(userItemMatrix.size(), allSpotIds.size());

            log.info(
                "相似度矩阵更新完成：景点数={}，用户数={}，缓存时长={}小时，Top-K={}",
                allSpotIds.size(),
                userItemMatrix.size(),
                simTTL,
                topK
            );
        } finally {
            computing.set(false);
        }
    }

    private double defaultDouble(Double value, double fallback) {
        return value == null ? fallback : value;
    }

    private int defaultInt(Integer value, int fallback) {
        return value == null ? fallback : value;
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

    private int getCandidateExpandFactor(RecommendationAlgorithmConfigDTO config) {
        return Math.max(defaultInt(config.getCandidateExpandFactor(), 2), 1);
    }

    private int getColdStartExpandFactor(RecommendationAlgorithmConfigDTO config) {
        return Math.max(defaultInt(config.getColdStartExpandFactor(), 3), 1);
    }

    // 推荐结果组装与元数据补充

    private RecommendationResponse buildRecommendationResponse(List<Long> spotIds, Integer limit, String type, Boolean needPreference) {
        return buildRecommendationResponse(spotIds, null, limit, type, needPreference, null);
    }

    private RecommendationResponse buildRecommendationResponse(List<Long> spotIds, Map<Long, Double> scoreMap, Integer limit, String type, Boolean needPreference) {
        return buildRecommendationResponse(spotIds, scoreMap, limit, type, needPreference, null);
    }

    private RecommendationResponse buildRecommendationResponse(List<Long> spotIds, Map<Long, Double> scoreMap,
                                                               Integer limit, String type, Boolean needPreference,
                                                               RecommendationResponse.DebugInfo debugInfo) {
        return recommendationQuerySupport.buildRecommendationResponse(
            spotIds,
            scoreMap,
            limit,
            type,
            needPreference,
            debugInfo
        );
    }

    private RecommendationResponse buildRecommendationResponse(List<Long> spotIds, Integer limit, String type, Boolean needPreference,
                                                               RecommendationResponse.DebugInfo debugInfo) {
        return buildRecommendationResponse(spotIds, null, limit, type, needPreference, debugInfo);
    }

    private Map<Long, String> getCategoryMap() {
        return recommendationQuerySupport.getCategoryMap();
    }

    private Map<Long, String> getRegionMap() {
        return recommendationQuerySupport.getRegionMap();
    }

    private void logRecommendationPreview(Long userId, RecommendationResponse response, boolean refresh) {
        recommendationScoreSupport.logRecommendationPreview(userId, response, refresh);
    }

    // 调试信息组装与日志输出

    private RecommendationResponse.DebugInfo initDebugInfo(Long userId, Integer limit, boolean refresh) {
        return recommendationScoreSupport.initDebugInfo(userId, limit, refresh);
    }

    private void populateInteractionDebugInfo(RecommendationResponse.DebugInfo debugInfo, Map<Long, Double> userInteractions) {
        recommendationScoreSupport.populateInteractionDebugInfo(debugInfo, userInteractions);
    }

    private void populateBehaviorStats(RecommendationResponse.DebugInfo debugInfo, Long userId) {
        recommendationScoreSupport.populateBehaviorStats(debugInfo, userId);
    }

    private void populateBehaviorDetails(RecommendationResponse.DebugInfo debugInfo, Long userId,
                                         RecommendationAlgorithmConfigDTO config) {
        recommendationScoreSupport.populateBehaviorDetails(debugInfo, userId, config);
    }

    private void populateScoreDebugEntries(RecommendationResponse.DebugInfo debugInfo, Map<Long, Double> scores, String description) {
        recommendationScoreSupport.populateScoreDebugEntries(debugInfo, scores, description);
    }

    private void populateFilteredScoresDebugEntries(RecommendationResponse.DebugInfo debugInfo, Map<Long, Double> scores, String description) {
        recommendationScoreSupport.populateFilteredScoresDebugEntries(debugInfo, scores, description);
    }

    private void populateRerankedScoreDebugEntries(RecommendationResponse.DebugInfo debugInfo, Map<Long, Double> scores, String description) {
        recommendationScoreSupport.populateRerankedScoreDebugEntries(debugInfo, scores, description);
    }

    private void populateFilteredOutDebugEntries(RecommendationResponse.DebugInfo debugInfo, List<Long> originalIds,
                                                 List<Long> filteredIds, String description) {
        recommendationScoreSupport.populateFilteredOutDebugEntries(debugInfo, originalIds, filteredIds, description);
    }

    private List<RecommendationResponse.ResultContribution> buildResultContributions(
        Map<Long, Double> finalScores,
        Map<Long, List<RecommendationResponse.DebugEntry>> contributionMap
    ) {
        return recommendationScoreSupport.buildResultContributions(finalScores, contributionMap);
    }

    private void logUserInteractionWeights(Long userId, Map<Long, Double> userInteractions, boolean debug) {
        recommendationScoreSupport.logUserInteractionWeights(userId, userInteractions, debug);
    }

    private void logScoreMap(String stage, Map<Long, Double> scoreMap, boolean debug) {
        recommendationScoreSupport.logScoreMap(stage, scoreMap, debug);
    }

    private void logFilteredRecommendations(Long userId, List<Long> originalIds, List<Long> filteredIds, boolean debug) {
        recommendationScoreSupport.logFilteredRecommendations(userId, originalIds, filteredIds, debug);
    }

    private void logColdStartResult(Long userId, String type, List<Long> categoryIds, List<Long> spotIds, boolean debug) {
        recommendationScoreSupport.logColdStartResult(userId, type, categoryIds, spotIds, debug);
    }

    private void logHeatRerankDetails(Map<Long, Double> beforeScores, Map<Long, Double> afterScores,
                                      Map<Long, Integer> heatMap, double rerankFactor, int maxHeat) {
        recommendationScoreSupport.logHeatRerankDetails(beforeScores, afterScores, heatMap, rerankFactor, maxHeat);
    }

    private String getSpotName(Long spotId) {
        return recommendationQuerySupport.getSpotName(spotId);
    }

    private Map<Long, Double> orderScoresByIds(List<Long> orderedIds, Map<Long, Double> scoreMap) {
        return recommendationScoreSupport.orderScoresByIds(orderedIds, scoreMap);
    }

    private Map<Long, Double> castScoreMap(Map<?, ?> rawMap) {
        return recommendationScoreSupport.castScoreMap(rawMap);
    }

    // ==================== 配置管理与状态查询 ====================

    @Override
    public RecommendationConfigBundleDTO getConfig() {
        return recommendationConfigSupport.getConfig();
    }

    @Override
    public void updateConfig(RecommendationConfigBundleDTO config) {
        recommendationConfigSupport.updateConfig(config);
        log.info("推荐算法配置已更新 {}", config);
    }

    @Override
    public RecommendationStatusDTO getStatus() {
        return recommendationConfigSupport.buildStatus(computing.get());
    }

    @Override
    public SimilarityPreviewResponse previewSimilarityNeighbors(Long spotId, Integer limit) {
        return recommendationSimilaritySupport.buildSimilarityPreview(spotId, limit, getStatus().getLastUpdateTime());
    }
}



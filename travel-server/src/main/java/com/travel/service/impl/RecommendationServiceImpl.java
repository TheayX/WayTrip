package com.travel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.travel.dto.home.HotSpotResponse;
import com.travel.dto.recommendation.RecommendationConfigBundleDTO;
import com.travel.dto.recommendation.LegacyRecommendationConfigDTO;
import com.travel.dto.recommendation.RecommendationResponse;
import com.travel.dto.recommendation.RecommendationStatusDTO;
import com.travel.dto.recommendation.SimilarityPreviewResponse;
import com.travel.entity.*;
import com.travel.enums.OrderStatus;
import com.travel.mapper.*;
import com.travel.service.cache.RecommendationCacheService;
import com.travel.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationServiceImpl implements RecommendationService {

    private final SpotMapper spotMapper;
    private final ReviewMapper reviewMapper;
    private final UserSpotFavoriteMapper userSpotFavoriteMapper;
    private final OrderMapper orderMapper;
    private final UserSpotViewMapper userSpotViewMapper;
    private final SpotCategoryMapper categoryMapper;
    private final SpotRegionMapper spotRegionMapper;
    private final UserPreferenceMapper userPreferenceMapper;
    private final RecommendationCacheService recommendationCacheService;

    private final AtomicBoolean computing = new AtomicBoolean(false);

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
    public RecommendationResponse previewRecommendations(Long userId, Integer limit, Boolean refresh, Boolean debug) {
        if (limit == null || limit <= 0) limit = 10;
        boolean refreshMode = Boolean.TRUE.equals(refresh);
        boolean debugMode = Boolean.TRUE.equals(debug);
        RecommendationResponse response = debugMode
            ? computeRecommendations(userId, limit, refreshMode, true)
            : (refreshMode ? refreshRecommendations(userId, limit) : getRecommendations(userId, limit));
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

    private RecommendationResponse computeRecommendations(Long userId, Integer limit) {
        return computeRecommendations(userId, limit, false, false);
    }

    private RecommendationResponse computeRecommendations(Long userId, Integer limit, boolean refresh) {
        return computeRecommendations(userId, limit, refresh, false);
    }

    private RecommendationResponse computeRecommendations(Long userId, Integer limit, boolean refresh, boolean debug) {
        LegacyRecommendationConfigDTO config = recommendationCacheService.loadLegacyConfig();
        RecommendationResponse.DebugInfo debugInfo = debug ? initDebugInfo(userId, limit, refresh) : null;

        log.info(
            "开始计算推荐结果：用户ID={}，请求数量={}，是否刷新={}，是否调试={}，协同过滤最少交互数={}，候选扩容倍数={}",
            userId,
            limit,
            refresh,
            debug,
            config.getMinInteractionsForCF(),
            getCandidateExpandFactor(config)
        );

        // ============ 根据总交互数判断是否走冷启动 ============
        Map<Long, Double> userInteractions = buildUserInteractionWeights(userId, config);
        populateBehaviorStats(debugInfo, userId);
        populateInteractionDebugInfo(debugInfo, userInteractions);
        logUserInteractionWeights(userId, userInteractions, debug);

        if (userInteractions.size() < config.getMinInteractionsForCF()) {
            log.info(
                "降级为冷启动：用户ID={}，交互景点数={}，阈值={}，原因=交互不足",
                userId,
                userInteractions.size(),
                config.getMinInteractionsForCF()
            );
            if (debugInfo != null) {
                debugInfo.setTriggerReason("交互数量不足，切换为冷启动");
                debugInfo.setNotes(List.of(
                    "用户交互景点数低于协同过滤阈值。",
                    "请检查用户历史行为是否足够，或适当调低 minInteractionsForCF。"
                ));
            }
            return handleColdStart(userId, limit, refresh, debug, debugInfo);
        }

        // 基于 ItemCF 计算个性化推荐。
        int candidateLimit = limit * getCandidateExpandFactor(config);
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
        filteredScores = applyHeatRerank(filteredScores, config, debug);
        populateRerankedScoreDebugEntries(debugInfo, filteredScores, "热度重排后的候选分数");
        filteredIds = new ArrayList<>(filteredScores.keySet());
        
        if (filteredIds.isEmpty()) {
            log.info("个性化推荐结果过滤后为空，降级为冷启动：用户ID={}", userId);
            if (debugInfo != null) {
                debugInfo.setTriggerReason("协同过滤候选集在过滤后为空，降级为冷启动");
            }
            return handleColdStart(userId, limit, refresh, debug, debugInfo);
        }

        // 缓存推荐分数。
        if (refresh) {
            filteredIds = rotateRecommendations(filteredIds, limit);
            filteredScores = orderScoresByIds(filteredIds, filteredScores);
        }

        recommendationCacheService.saveUserRecommendation(userId, filteredScores, config.getUserRecTTLMinutes());

        log.info(
            "推荐结果计算完成：用户ID={}，推荐类型=personalized，候选数={}，最终返回数={}，缓存时长={}分钟",
            userId,
            recommendedScores.size(),
            Math.min(filteredIds.size(), limit),
            config.getUserRecTTLMinutes()
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

    /**
     * 构建单个用户的融合交互权重：Map<spotId, weight>。
     */
    private Map<Long, Double> buildUserInteractionWeights(Long userId, LegacyRecommendationConfigDTO config) {
        Map<Long, Double> weights = new HashMap<>();

        // 浏览行为。
        List<UserSpotView> views = userSpotViewMapper.selectList(
            new LambdaQueryWrapper<UserSpotView>()
                .eq(UserSpotView::getUserId, userId)
                .select(UserSpotView::getSpotId, UserSpotView::getViewSource, UserSpotView::getViewDuration)
        );
        for (UserSpotView v : views) {
            mergeInteractionWeight(weights, v.getSpotId(), calculateViewWeight(v, config));
        }

        // 收藏行为。
        List<UserSpotFavorite> favorites = userSpotFavoriteMapper.selectList(
            new LambdaQueryWrapper<UserSpotFavorite>()
                .eq(UserSpotFavorite::getUserId, userId)
                .eq(UserSpotFavorite::getIsDeleted, 0)
                .select(UserSpotFavorite::getSpotId)
        );
        for (UserSpotFavorite f : favorites) {
            mergeInteractionWeight(weights, f.getSpotId(), config.getWeightFavorite());
        }

        // 评分行为。
        List<Review> reviews = reviewMapper.selectList(
            new LambdaQueryWrapper<Review>()
                .eq(Review::getUserId, userId)
                .eq(Review::getIsDeleted, 0)
                .select(Review::getSpotId, Review::getScore)
        );
        for (Review r : reviews) {
            double w = r.getScore() * config.getWeightReviewFactor();
            mergeInteractionWeight(weights, r.getSpotId(), w);
        }

        // 已支付和已完成订单。
        List<Order> orders = orderMapper.selectList(
            new LambdaQueryWrapper<Order>()
                .eq(Order::getUserId, userId)
                .eq(Order::getIsDeleted, 0)
                .in(Order::getStatus, OrderStatus.PAID.getCode(), OrderStatus.COMPLETED.getCode())
                .select(Order::getSpotId, Order::getStatus)
        );
        for (Order o : orders) {
            double w = o.getStatus() == OrderStatus.COMPLETED.getCode() ? config.getWeightOrderCompleted() : config.getWeightOrderPaid();
            mergeInteractionWeight(weights, o.getSpotId(), w);
        }

        return weights;
    }


    /**
     * 冷启动处理：基于用户偏好或热门景点返回结果。
     */
    private RecommendationResponse handleColdStart(Long userId, Integer limit) {
        return handleColdStart(userId, limit, false);
    }

    /**
     * 冷启动处理：支持刷新时轮换结果。
     */
    private RecommendationResponse handleColdStart(Long userId, Integer limit, boolean refresh) {
        return handleColdStart(userId, limit, refresh, false);
    }

    private RecommendationResponse handleColdStart(Long userId, Integer limit, boolean refresh, boolean debug) {
        return handleColdStart(userId, limit, refresh, debug, debug ? initDebugInfo(userId, limit, refresh) : null);
    }

    private RecommendationResponse handleColdStart(Long userId, Integer limit, boolean refresh, boolean debug,
                                                   RecommendationResponse.DebugInfo debugInfo) {
        LegacyRecommendationConfigDTO config = recommendationCacheService.loadLegacyConfig();
        List<Long> categoryIds = getUserPreferenceCategoryIds(userId);

        // 如果用户设置了偏好标签，则优先按偏好推荐。
        if (!categoryIds.isEmpty()) {
            List<Spot> spots = spotMapper.selectList(
                new LambdaQueryWrapper<Spot>()
                    .eq(Spot::getIsPublished, 1)
                    .in(Spot::getCategoryId, categoryIds)
                    .eq(Spot::getIsDeleted, 0)
                    .orderByDesc(Spot::getHeatScore)
                    .last("LIMIT " + (refresh ? Math.max(limit * getColdStartExpandFactor(config), limit) : limit))
            );

            List<Long> spotIds = spots.stream().map(Spot::getId).collect(Collectors.toList());
            if (refresh) {
                spotIds = rotateRecommendations(spotIds, limit);
            }
            if (debugInfo != null) {
                debugInfo.setTriggerReason("交互不足，但存在用户偏好标签");
                debugInfo.setFinalCount(Math.min(spotIds.size(), limit));
                debugInfo.setNotes(List.of(
                    "当前结果来自偏好冷启动链路。",
                    "请检查用户偏好标签与返回景点分类是否匹配。"
                ));
                debugInfo.setExtra(Map.of("categoryIds", categoryIds));
            }
            logColdStartResult(userId, "preference", categoryIds, spotIds, debug);
            return buildRecommendationResponse(spotIds, limit, "preference", false, debugInfo);
        }

        // 没有偏好标签时，回退到热门结果并提示设置偏好。
        HotSpotResponse hotSpots = getHotSpots(refresh ? Math.max(limit * getColdStartExpandFactor(config), limit) : limit);
        List<HotSpotResponse.SpotItem> hotSpotList = new ArrayList<>(hotSpots.getList());
        if (refresh) {
            rotateSpotItems(hotSpotList, limit);
        }
        if (debugInfo != null) {
            debugInfo.setTriggerReason("交互不足且没有偏好标签");
            debugInfo.setFinalCount(Math.min(hotSpotList.size(), limit));
            debugInfo.setNotes(List.of(
                "当前结果来自热门兜底链路。",
                "请检查热度分数、上架状态和冷启动策略。"
            ));
        }
        logColdStartResult(
            userId,
            "hot",
            Collections.emptyList(),
            hotSpotList.stream().map(HotSpotResponse.SpotItem::getId).collect(Collectors.toList()),
            debug
        );
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
    }

    private List<Long> getUserPreferenceCategoryIds(Long userId) {
        List<UserPreference> preferences = userPreferenceMapper.selectList(
            new LambdaQueryWrapper<UserPreference>()
                .eq(UserPreference::getUserId, userId)
                .eq(UserPreference::getIsDeleted, 0)
        );
        if (preferences.isEmpty()) {
            return Collections.emptyList();
        }

        return preferences.stream()
            .map(UserPreference::getTag)
            .map(this::parsePreferenceCategoryId)
            .filter(Objects::nonNull)
            .distinct()
            .collect(Collectors.toList());
    }

    private Long parsePreferenceCategoryId(String tag) {
        if (tag == null || tag.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(tag.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
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
            Map<Long, Double> similarities = getSimilarSpots(spotId);
            
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

    private Map<Long, Double> applyHeatRerank(Map<Long, Double> scoreMap, LegacyRecommendationConfigDTO config) {
        return applyHeatRerank(scoreMap, config, false);
    }

    private Map<Long, Double> applyHeatRerank(Map<Long, Double> scoreMap, LegacyRecommendationConfigDTO config, boolean debug) {
        if (scoreMap == null || scoreMap.isEmpty()) {
            return Collections.emptyMap();
        }

        double rerankFactor = config.getHeatRerankFactor() == null ? 0.0 : config.getHeatRerankFactor();
        if (rerankFactor <= 0) {
            if (debug) {
                log.info("跳过热度重排：原因=热度重排系数小于等于0，当前系数={}", rerankFactor);
            }
            return new LinkedHashMap<>(scoreMap);
        }

        List<Spot> spots = spotMapper.selectBatchIds(scoreMap.keySet());
        Map<Long, Integer> heatMap = spots.stream()
            .filter(spot -> spot.getIsDeleted() == 0 && spot.getIsPublished() == 1)
            .collect(Collectors.toMap(Spot::getId, spot -> Optional.ofNullable(spot.getHeatScore()).orElse(0)));

        int maxHeat = heatMap.values().stream().max(Integer::compareTo).orElse(0);
        if (maxHeat <= 0) {
            if (debug) {
                log.info("跳过热度重排：原因=候选景点热度均为空或为0");
            }
            return new LinkedHashMap<>(scoreMap);
        }

        Map<Long, Double> rerankedScores = scoreMap.entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> entry.getValue() + rerankFactor * (heatMap.getOrDefault(entry.getKey(), 0) / (double) maxHeat),
                (left, right) -> left,
                LinkedHashMap::new
            ))
            .entrySet().stream()
            .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                (left, right) -> left,
                LinkedHashMap::new
            ));

        if (debug) {
            logHeatRerankDetails(scoreMap, rerankedScores, heatMap, rerankFactor, maxHeat);
        }
        return rerankedScores;
    }

    /**
     * 从 Redis 读取相似景点。
     */
    @SuppressWarnings("unchecked")
    private Map<Long, Double> getSimilarSpots(Long spotId) {
        Object cached = recommendationCacheService.getSimilarity(spotId);
        if (!(cached instanceof Map<?, ?> rawMap) || rawMap.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Long, Double> similarities = new HashMap<>();
        for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
            Long similarSpotId = castToLong(entry.getKey());
            Double similarity = castToDouble(entry.getValue());
            if (similarSpotId != null && similarity != null) {
                similarities.put(similarSpotId, similarity);
            }
        }
        return similarities;
    }

    private Long castToLong(Object value) {
        if (value instanceof Long longValue) {
            return longValue;
        }
        if (value instanceof Integer intValue) {
            return intValue.longValue();
        }
        if (value instanceof Number numberValue) {
            return numberValue.longValue();
        }
        if (value instanceof String stringValue && !stringValue.isBlank()) {
            try {
                return Long.parseLong(stringValue);
            } catch (NumberFormatException e) {
                log.warn("Redis Key 转 Long 失败：{}", stringValue);
            }
        }
        return null;
    }

    private Double castToDouble(Object value) {
        if (value instanceof Double doubleValue) {
            return doubleValue;
        }
        if (value instanceof Float floatValue) {
            return floatValue.doubleValue();
        }
        if (value instanceof Number numberValue) {
            return numberValue.doubleValue();
        }
        if (value instanceof String stringValue && !stringValue.isBlank()) {
            try {
                return Double.parseDouble(stringValue);
            } catch (NumberFormatException e) {
                log.warn("Redis 值转 Double 失败：{}", stringValue);
            }
        }
        return null;
    }

    /**
     * 过滤用户已经交互过的景点。
     */
    private List<Long> filterInteractedSpots(Long userId, List<Long> spotIds) {
        if (spotIds.isEmpty()) return spotIds;

        // 已评分
        Set<Long> ratedIds = reviewMapper.selectList(
            new LambdaQueryWrapper<Review>()
                .eq(Review::getUserId, userId)
                .eq(Review::getIsDeleted, 0)
        ).stream().map(Review::getSpotId).collect(Collectors.toSet());

        // 已收藏
        Set<Long> favoriteIds = userSpotFavoriteMapper.selectList(
            new LambdaQueryWrapper<UserSpotFavorite>()
                .eq(UserSpotFavorite::getUserId, userId)
                .eq(UserSpotFavorite::getIsDeleted, 0)
        ).stream().map(UserSpotFavorite::getSpotId).collect(Collectors.toSet());

        // 已下单（不含已取消）
        Set<Long> orderedIds = orderMapper.selectList(
            new LambdaQueryWrapper<Order>()
                .eq(Order::getUserId, userId)
                .eq(Order::getIsDeleted, 0)
                .ne(Order::getStatus, OrderStatus.CANCELLED.getCode())
        ).stream().map(Order::getSpotId).collect(Collectors.toSet());

        Set<Long> excludeIds = new HashSet<>();
        excludeIds.addAll(ratedIds);
        excludeIds.addAll(favoriteIds);
        excludeIds.addAll(orderedIds);

        return spotIds.stream()
            .filter(id -> !excludeIds.contains(id))
            .collect(Collectors.toList());
    }


    @Override
    public HotSpotResponse getHotSpots(Integer limit) {
        if (limit == null || limit <= 0) limit = 10;

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

        return response;
    }

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
            LegacyRecommendationConfigDTO config = recommendationCacheService.loadLegacyConfig();
            log.info("开始更新相似度矩阵");
            Set<Long> activeSpotIds = getActiveSpotIds();
            if (activeSpotIds.isEmpty()) {
                log.info("没有有效上架景点，跳过相似度矩阵更新");
                return;
            }

            // ============ 第 1 步：构建全量用户-景点交互矩阵 ============
            // Map<userId, Map<spotId, weight>>
            Map<Long, Map<Long, Double>> userItemMatrix = new HashMap<>();
            Set<Long> allSpotIds = new HashSet<>();

            // 1a. 浏览行为
            List<UserSpotView> allViews = userSpotViewMapper.selectList(
                new LambdaQueryWrapper<UserSpotView>()
                    .select(UserSpotView::getUserId, UserSpotView::getSpotId, UserSpotView::getViewSource, UserSpotView::getViewDuration)
            );
            log.info("离线矩阵更新：已读取浏览行为 {} 条", allViews.size());
            for (UserSpotView v : allViews) {
                if (!activeSpotIds.contains(v.getSpotId())) continue;
                mergeInteractionWeight(
                    userItemMatrix.computeIfAbsent(v.getUserId(), k -> new HashMap<>()),
                    v.getSpotId(),
                    calculateViewWeight(v, config)
                );
                allSpotIds.add(v.getSpotId());
            }

            // 1b. 收藏行为
            List<UserSpotFavorite> allFavorites = userSpotFavoriteMapper.selectList(
                new LambdaQueryWrapper<UserSpotFavorite>()
                    .eq(UserSpotFavorite::getIsDeleted, 0)
                    .select(UserSpotFavorite::getUserId, UserSpotFavorite::getSpotId)
            );
            log.info("离线矩阵更新：已读取收藏行为 {} 条", allFavorites.size());
            for (UserSpotFavorite f : allFavorites) {
                if (!activeSpotIds.contains(f.getSpotId())) continue;
                mergeInteractionWeight(
                    userItemMatrix.computeIfAbsent(f.getUserId(), k -> new HashMap<>()),
                    f.getSpotId(),
                    config.getWeightFavorite()
                );
                allSpotIds.add(f.getSpotId());
            }

            // 1c. 评分行为
            List<Review> allRatings = reviewMapper.selectList(
                new LambdaQueryWrapper<Review>()
                    .eq(Review::getIsDeleted, 0)
                    .select(Review::getUserId, Review::getSpotId, Review::getScore)
            );
            log.info("离线矩阵更新：已读取评分行为 {} 条", allRatings.size());
            for (Review r : allRatings) {
                if (!activeSpotIds.contains(r.getSpotId())) continue;
                double w = r.getScore() * config.getWeightReviewFactor();
                mergeInteractionWeight(
                    userItemMatrix.computeIfAbsent(r.getUserId(), k -> new HashMap<>()),
                    r.getSpotId(),
                    w
                );
                allSpotIds.add(r.getSpotId());
            }

            // 1d. 已支付和已完成订单
            List<Order> allOrders = orderMapper.selectList(
                new LambdaQueryWrapper<Order>()
                    .eq(Order::getIsDeleted, 0)
                    .in(Order::getStatus, OrderStatus.PAID.getCode(), OrderStatus.COMPLETED.getCode())
                    .select(Order::getUserId, Order::getSpotId, Order::getStatus)
            );
            log.info("离线矩阵更新：已读取订单行为 {} 条", allOrders.size());
            for (Order o : allOrders) {
                if (!activeSpotIds.contains(o.getSpotId())) continue;
                double w = o.getStatus() == OrderStatus.COMPLETED.getCode() ? config.getWeightOrderCompleted() : config.getWeightOrderPaid();
                mergeInteractionWeight(
                    userItemMatrix.computeIfAbsent(o.getUserId(), k -> new HashMap<>()),
                    o.getSpotId(),
                    w
                );
                allSpotIds.add(o.getSpotId());
            }

            if (userItemMatrix.isEmpty()) {
                log.info("没有交互数据，跳过相似度矩阵更新");
                return;
            }

            log.info("交互矩阵构建完成：用户数={}，景点数={}", userItemMatrix.size(), allSpotIds.size());
            logUserItemMatrixSamples(userItemMatrix);

            // ============ 第 2 步：预计算 IUF 所需的 |N(u)| ============
            Map<Long, Integer> userActivityCount = new HashMap<>();
            for (Map.Entry<Long, Map<Long, Double>> entry : userItemMatrix.entrySet()) {
                userActivityCount.put(entry.getKey(), entry.getValue().size());
            }
            logUserActivitySamples(userActivityCount);

            // ============ 第 3 步：构建景点到用户的倒排索引 ============
            // Map<spotId, Set<userId>> 表示 N(i)
            Map<Long, Set<Long>> spotUserSets = new HashMap<>();
            for (Map.Entry<Long, Map<Long, Double>> entry : userItemMatrix.entrySet()) {
                Long userId = entry.getKey();
                for (Long spotId : entry.getValue().keySet()) {
                    spotUserSets.computeIfAbsent(spotId, k -> new HashSet<>()).add(userId);
                }
            }

            // ============ 第 4 步：计算 IUF 加权相似度 ============
            List<Long> spotIdList = new ArrayList<>(allSpotIds);
            int topK = config.getTopKNeighbors();
            int simTTL = config.getSimilarityTTLHours();

            for (int i = 0; i < spotIdList.size(); i++) {
                Long spotI = spotIdList.get(i);
                Set<Long> usersI = spotUserSets.getOrDefault(spotI, Collections.emptySet());
                if (usersI.isEmpty()) continue;

                Map<Long, Double> similarities = new HashMap<>();

                for (int j = 0; j < spotIdList.size(); j++) {
                    if (i == j) continue;

                    Long spotJ = spotIdList.get(j);
                    Set<Long> usersJ = spotUserSets.getOrDefault(spotJ, Collections.emptySet());
                    if (usersJ.isEmpty()) continue;

                    // 计算 IUF 加权相似度。
                    double similarity = computeIUFSimilarity(usersI, usersJ, userActivityCount);

                    if (similarity > 0) {
                        similarities.put(spotJ, similarity);
                    }
                }

                // 仅保留 Top-K 相似景点。
                Map<Long, Double> topSimilarities = similarities.entrySet().stream()
                    .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                    .limit(topK)
                    .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                    ));

                // 写入 Redis 缓存。
                recommendationCacheService.saveSimilarity(spotI, java.util.Objects.requireNonNull(topSimilarities), simTTL);
                logSpotSimilaritySummary(spotI, topSimilarities);
            }

            // ============ 保存状态摘要 ============
            Map<String, Object> statusMap = new HashMap<>();
            statusMap.put("lastUpdateTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            statusMap.put("totalUsers", userItemMatrix.size());
            statusMap.put("totalSpots", allSpotIds.size());
            recommendationCacheService.saveStatus(statusMap);

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

    private Set<Long> getActiveSpotIds() {
        return spotMapper.selectList(
                new LambdaQueryWrapper<Spot>()
                        .eq(Spot::getIsPublished, 1)
                        .eq(Spot::getIsDeleted, 0)
                        .select(Spot::getId)
        ).stream()
                .map(Spot::getId)
                .collect(Collectors.toSet());
    }

    /**
     * 计算 IUF 加权余弦相似度。
     *
     * @param usersI 与景点 i 交互过的用户集合，即 N(i)
     * @param usersJ 与景点 j 交互过的用户集合，即 N(j)
     * @param userActivityCount 每个用户的总交互景点数，即 |N(u)|
     */
    private double computeIUFSimilarity(Set<Long> usersI, Set<Long> usersJ,
                                         Map<Long, Integer> userActivityCount) {
        // 优先遍历较小集合，减少计算量。
        Set<Long> smaller = usersI.size() < usersJ.size() ? usersI : usersJ;
        Set<Long> larger = smaller == usersI ? usersJ : usersI;

        double iufSum = 0.0;
        for (Long userId : smaller) {
            if (larger.contains(userId)) {
                int nu = userActivityCount.getOrDefault(userId, 1);
                iufSum += 1.0 / Math.log(1 + nu);
            }
        }

        if (iufSum == 0) return 0;

        double denominator = Math.sqrt(usersI.size()) * Math.sqrt(usersJ.size());
        return iufSum / denominator;
    }

    private double calculateViewWeight(UserSpotView view, LegacyRecommendationConfigDTO config) {
        double baseWeight = config.getWeightView() == null ? 0.5 : config.getWeightView();
        return baseWeight * getViewSourceFactor(view.getViewSource(), config) * getViewDurationFactor(view.getViewDuration(), config);
    }

    private void mergeInteractionWeight(Map<Long, Double> weights, Long spotId, Double weight) {
        if (weights == null || spotId == null || weight == null || weight <= 0) {
            return;
        }
        weights.merge(spotId, weight, Double::sum);
    }

    private double getViewSourceFactor(String source, LegacyRecommendationConfigDTO config) {
        if (source == null || source.isBlank()) {
            return defaultDouble(config.getViewSourceFactorDetail(), 1.0);
        }
        return switch (source.trim().toLowerCase(Locale.ROOT)) {
            case "search" -> defaultDouble(config.getViewSourceFactorSearch(), 1.2);
            case "recommend" -> defaultDouble(config.getViewSourceFactorRecommend(), 1.1);
            case "home" -> defaultDouble(config.getViewSourceFactorHome(), 0.9);
            case "guide" -> defaultDouble(config.getViewSourceFactorGuide(), 1.0);
            case "detail" -> defaultDouble(config.getViewSourceFactorDetail(), 1.0);
            default -> defaultDouble(config.getViewSourceFactorDetail(), 1.0);
        };
    }

    private double getViewDurationFactor(Integer duration, LegacyRecommendationConfigDTO config) {
        int seconds = duration == null ? 0 : Math.max(duration, 0);
        int shortThreshold = defaultInt(config.getViewDurationShortThresholdSeconds(), 10);
        int mediumThreshold = Math.max(shortThreshold, defaultInt(config.getViewDurationMediumThresholdSeconds(), 60));
        int longThreshold = Math.max(mediumThreshold, defaultInt(config.getViewDurationLongThresholdSeconds(), 180));

        if (seconds < shortThreshold) {
            return defaultDouble(config.getViewDurationFactorShort(), 0.6);
        }
        if (seconds < mediumThreshold) {
            return defaultDouble(config.getViewDurationFactorMedium(), 1.0);
        }
        if (seconds < longThreshold) {
            return defaultDouble(config.getViewDurationFactorLong(), 1.2);
        }
        return defaultDouble(config.getViewDurationFactorVeryLong(), 1.35);
    }

    private double defaultDouble(Double value, double fallback) {
        return value == null ? fallback : value;
    }

    private int defaultInt(Integer value, int fallback) {
        return value == null ? fallback : value;
    }

    private int getCandidateExpandFactor(LegacyRecommendationConfigDTO config) {
        return Math.max(defaultInt(config.getCandidateExpandFactor(), 2), 1);
    }

    private int getColdStartExpandFactor(LegacyRecommendationConfigDTO config) {
        return Math.max(defaultInt(config.getColdStartExpandFactor(), 3), 1);
    }

    private RecommendationResponse buildRecommendationResponse(List<Long> spotIds, Integer limit, String type, Boolean needPreference) {
        return buildRecommendationResponse(spotIds, null, limit, type, needPreference, null);
    }

    private RecommendationResponse buildRecommendationResponse(List<Long> spotIds, Map<Long, Double> scoreMap, Integer limit, String type, Boolean needPreference) {
        return buildRecommendationResponse(spotIds, scoreMap, limit, type, needPreference, null);
    }

    private RecommendationResponse buildRecommendationResponse(List<Long> spotIds, Map<Long, Double> scoreMap,
                                                               Integer limit, String type, Boolean needPreference,
                                                               RecommendationResponse.DebugInfo debugInfo) {
        List<Long> limitedIds = spotIds.stream().limit(limit).collect(Collectors.toList());
        
        if (limitedIds.isEmpty()) {
            RecommendationResponse response = new RecommendationResponse();
            response.setType(type);
            response.setList(Collections.emptyList());
            response.setNeedPreference(needPreference);
            response.setDebugInfo(debugInfo);
            return response;
        }

        List<Spot> spots = spotMapper.selectBatchIds(limitedIds);
        Map<Long, String> categoryMap = getCategoryMap();
        Map<Long, String> regionMap = getRegionMap();

        // 保持原有顺序
        Map<Long, Spot> spotMap = spots.stream().collect(Collectors.toMap(Spot::getId, s -> s));

        RecommendationResponse response = new RecommendationResponse();
        response.setType(type);
        response.setNeedPreference(needPreference);
        if (debugInfo != null) {
            debugInfo.setFinalCount(limitedIds.size());
        }
        response.setDebugInfo(debugInfo);
        response.setList(limitedIds.stream()
            .map(spotMap::get)
            .filter(spot -> spot != null && spot.getIsDeleted() == 0 && spot.getIsPublished() == 1)
            .map(spot -> {
                RecommendationResponse.SpotItem item = new RecommendationResponse.SpotItem();
                item.setId(spot.getId());
                item.setName(spot.getName());
                item.setCoverImage(spot.getCoverImageUrl());
                item.setPrice(spot.getPrice());
                item.setAvgRating(spot.getAvgRating());
                item.setRatingCount(spot.getRatingCount());
                item.setCategoryName(categoryMap.get(spot.getCategoryId()));
                item.setRegionName(regionMap.get(spot.getRegionId()));
                item.setScore(scoreMap == null ? null : scoreMap.get(spot.getId()));
                return item;
            })
            .collect(Collectors.toList()));

        return response;
    }

    private RecommendationResponse buildRecommendationResponse(List<Long> spotIds, Integer limit, String type, Boolean needPreference,
                                                               RecommendationResponse.DebugInfo debugInfo) {
        return buildRecommendationResponse(spotIds, null, limit, type, needPreference, debugInfo);
    }

    private Map<Long, String> getCategoryMap() {
        return categoryMapper.selectList(new LambdaQueryWrapper<SpotCategory>().eq(SpotCategory::getIsDeleted, 0)).stream()
            .collect(Collectors.toMap(SpotCategory::getId, SpotCategory::getName));
    }

    private Map<Long, String> getRegionMap() {
        return spotRegionMapper.selectList(new LambdaQueryWrapper<SpotRegion>().eq(SpotRegion::getIsDeleted, 0)).stream()
            .collect(Collectors.toMap(SpotRegion::getId, SpotRegion::getName));
    }

    private void logRecommendationPreview(Long userId, RecommendationResponse response, boolean refresh) {
        if (response == null) {
            log.info("推荐调试预览：用户ID={}，是否刷新={}，结果为空", userId, refresh);
            return;
        }

        String items = Optional.ofNullable(response.getList())
            .orElseGet(Collections::emptyList)
            .stream()
            .map(item -> String.format(
                "{id=%d,name=%s,score=%s,category=%s,region=%s}",
                item.getId(),
                item.getName(),
                item.getScore() == null ? "null" : String.format("%.4f", item.getScore()),
                item.getCategoryName(),
                item.getRegionName()
            ))
            .collect(Collectors.joining(", "));

        log.info(
            "推荐调试预览结果：用户ID={}，是否刷新={}，推荐类型={}，是否需要偏好引导={}，结果明细=[{}]",
            userId,
            refresh,
            response.getType(),
            response.getNeedPreference(),
            items
        );
    }

    private RecommendationResponse.DebugInfo initDebugInfo(Long userId, Integer limit, boolean refresh) {
        RecommendationResponse.DebugInfo debugInfo = new RecommendationResponse.DebugInfo();
        debugInfo.setUserId(userId);
        debugInfo.setRequestLimit(limit);
        debugInfo.setRefresh(refresh);
        debugInfo.setDebugEnabled(true);
        return debugInfo;
    }

    private void populateInteractionDebugInfo(RecommendationResponse.DebugInfo debugInfo, Map<Long, Double> userInteractions) {
        if (debugInfo == null) {
            return;
        }
        debugInfo.setInteractionCount(userInteractions.size());
        debugInfo.setUserInteractions(toDebugEntries(userInteractions, "用户对该景点的融合交互权重"));
    }

    private void populateBehaviorStats(RecommendationResponse.DebugInfo debugInfo, Long userId) {
        if (debugInfo == null || userId == null) {
            return;
        }

        List<UserSpotView> views = userSpotViewMapper.selectList(
            new LambdaQueryWrapper<UserSpotView>()
                .eq(UserSpotView::getUserId, userId)
                .select(UserSpotView::getSpotId)
        );
        List<UserSpotFavorite> favorites = userSpotFavoriteMapper.selectList(
            new LambdaQueryWrapper<UserSpotFavorite>()
                .eq(UserSpotFavorite::getUserId, userId)
                .eq(UserSpotFavorite::getIsDeleted, 0)
                .select(UserSpotFavorite::getSpotId)
        );
        List<Review> reviews = reviewMapper.selectList(
            new LambdaQueryWrapper<Review>()
                .eq(Review::getUserId, userId)
                .eq(Review::getIsDeleted, 0)
                .select(Review::getSpotId)
        );
        List<Order> orders = orderMapper.selectList(
            new LambdaQueryWrapper<Order>()
                .eq(Order::getUserId, userId)
                .eq(Order::getIsDeleted, 0)
                .in(Order::getStatus, OrderStatus.PAID.getCode(), OrderStatus.COMPLETED.getCode())
                .select(Order::getSpotId)
        );

        debugInfo.setBehaviorStats(List.of(
            buildBehaviorStat("浏览", views.stream().map(UserSpotView::getSpotId).collect(Collectors.toList()), "来源表 user_spot_view，统计所有浏览记录"),
            buildBehaviorStat("收藏", favorites.stream().map(UserSpotFavorite::getSpotId).collect(Collectors.toList()), "仅统计 is_deleted = 0 的有效收藏"),
            buildBehaviorStat("评分", reviews.stream().map(Review::getSpotId).collect(Collectors.toList()), "仅统计 is_deleted = 0 的有效评分"),
            buildBehaviorStat("订单", orders.stream().map(Order::getSpotId).collect(Collectors.toList()), "仅统计 PAID 和 COMPLETED 的有效订单"),
            new RecommendationResponse.BehaviorStat(
                "合并后",
                null,
                debugInfo.getInteractionCount(),
                "四类行为按景点合并并加权后，最终进入 r_ui 计算的唯一景点数"
            )
        ));
    }

    private RecommendationResponse.BehaviorStat buildBehaviorStat(String behavior, List<Long> spotIds, String description) {
        Set<Long> uniqueSpotIds = spotIds == null ? Collections.emptySet() : new HashSet<>(spotIds);
        return new RecommendationResponse.BehaviorStat(
            behavior,
            spotIds == null ? 0 : spotIds.size(),
            uniqueSpotIds.size(),
            description
        );
    }

    private void populateScoreDebugEntries(RecommendationResponse.DebugInfo debugInfo, Map<Long, Double> scores, String description) {
        if (debugInfo == null) {
            return;
        }
        debugInfo.setCandidateCount(scores.size());
        debugInfo.setCandidateScores(toDebugEntries(scores, description));
    }

    private void populateFilteredScoresDebugEntries(RecommendationResponse.DebugInfo debugInfo, Map<Long, Double> scores, String description) {
        if (debugInfo == null) {
            return;
        }
        debugInfo.setFilteredCount(scores.size());
        debugInfo.setFilteredScores(toDebugEntries(scores, description));
    }

    private void populateRerankedScoreDebugEntries(RecommendationResponse.DebugInfo debugInfo, Map<Long, Double> scores, String description) {
        if (debugInfo == null) {
            return;
        }
        debugInfo.setRerankedScores(toDebugEntries(scores, description));
    }

    private void populateFilteredOutDebugEntries(RecommendationResponse.DebugInfo debugInfo, List<Long> originalIds,
                                                 List<Long> filteredIds, String description) {
        if (debugInfo == null) {
            return;
        }
        Set<Long> filteredSet = new HashSet<>(filteredIds);
        List<RecommendationResponse.DebugEntry> removedItems = originalIds.stream()
            .filter(id -> !filteredSet.contains(id))
            .map(id -> new RecommendationResponse.DebugEntry(id, getSpotName(id), null, description))
            .collect(Collectors.toList());
        debugInfo.setFilteredOutItems(removedItems);
    }

    private List<RecommendationResponse.DebugEntry> toDebugEntries(Map<Long, Double> scoreMap, String description) {
        if (scoreMap == null || scoreMap.isEmpty()) {
            return Collections.emptyList();
        }
        return scoreMap.entrySet().stream()
            .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
            .limit(30)
            .map(entry -> new RecommendationResponse.DebugEntry(
                entry.getKey(),
                getSpotName(entry.getKey()),
                entry.getValue(),
                description
            ))
            .collect(Collectors.toList());
    }

    private List<RecommendationResponse.ResultContribution> buildResultContributions(
        Map<Long, Double> finalScores,
        Map<Long, List<RecommendationResponse.DebugEntry>> contributionMap
    ) {
        if (finalScores == null || finalScores.isEmpty() || contributionMap == null || contributionMap.isEmpty()) {
            return Collections.emptyList();
        }
        return finalScores.entrySet().stream()
            .limit(20)
            .map(entry -> {
                List<RecommendationResponse.DebugEntry> contributors = contributionMap
                    .getOrDefault(entry.getKey(), Collections.emptyList())
                    .stream()
                    .sorted(Comparator.comparing(RecommendationResponse.DebugEntry::getScore, Comparator.nullsLast(Double::compareTo)).reversed())
                    .limit(5)
                    .collect(Collectors.toList());
                return new RecommendationResponse.ResultContribution(
                    entry.getKey(),
                    getSpotName(entry.getKey()),
                    entry.getValue(),
                    contributors
                );
            })
            .collect(Collectors.toList());
    }

    private void logUserInteractionWeights(Long userId, Map<Long, Double> userInteractions, boolean debug) {
        if (!debug) {
            log.info("用户交互权重构建完成：用户ID={}，交互景点数={}", userId, userInteractions.size());
            return;
        }
        log.info(
            "用户交互权重明细：用户ID={}，交互景点数={}，权重详情=[{}]",
            userId,
            userInteractions.size(),
            formatSpotScoreEntries(userInteractions, 20)
        );
    }

    private void logScoreMap(String stage, Map<Long, Double> scoreMap, boolean debug) {
        if (!debug || scoreMap == null) {
            return;
        }
        log.info("{}：候选数={}，明细=[{}]", stage, scoreMap.size(), formatSpotScoreEntries(scoreMap, 20));
    }

    private void logFilteredRecommendations(Long userId, List<Long> originalIds, List<Long> filteredIds, boolean debug) {
        if (!debug) {
            return;
        }
        Set<Long> filteredSet = new LinkedHashSet<>(filteredIds);
        List<Long> removedIds = originalIds.stream()
            .filter(id -> !filteredSet.contains(id))
            .collect(Collectors.toList());
        log.info(
            "候选过滤结果：用户ID={}，过滤前数量={}，过滤后数量={}，被过滤景点=[{}]",
            userId,
            originalIds.size(),
            filteredIds.size(),
            formatSpotIdList(removedIds, 20)
        );
    }

    private void logColdStartResult(Long userId, String type, List<Long> categoryIds, List<Long> spotIds, boolean debug) {
        String reason = "preference".equals(type) ? "存在用户偏好，按偏好分类推荐" : "缺少足够个性化信号，回退到热门推荐";
        log.info(
            "冷启动推荐结果：用户ID={}，类型={}，原因={}，偏好分类ID={}，候选景点=[{}]",
            userId,
            type,
            reason,
            categoryIds,
            formatSpotIdList(spotIds, debug ? 20 : 10)
        );
    }

    private void logHeatRerankDetails(Map<Long, Double> beforeScores, Map<Long, Double> afterScores,
                                      Map<Long, Integer> heatMap, double rerankFactor, int maxHeat) {
        String details = afterScores.entrySet().stream()
            .limit(20)
            .map(entry -> {
                Long spotId = entry.getKey();
                double before = beforeScores.getOrDefault(spotId, 0.0);
                int heat = heatMap.getOrDefault(spotId, 0);
                double after = entry.getValue();
                return String.format(
                    Locale.ROOT,
                    "{景点ID=%d,景点名称=%s,原始分数=%.4f,热度值=%d,热度加成=%.4f,重排后分数=%.4f}",
                    spotId,
                    getSpotName(spotId),
                    before,
                    heat,
                    after - before,
                    after
                );
            })
            .collect(Collectors.joining(", "));
        log.info(
            "热度重排结果：重排系数={}，最大热度={}，重排明细=[{}]",
            rerankFactor,
            maxHeat,
            details
        );
    }

    private void logUserItemMatrixSamples(Map<Long, Map<Long, Double>> userItemMatrix) {
        String samples = userItemMatrix.entrySet().stream()
            .sorted(Map.Entry.<Long, Map<Long, Double>>comparingByKey())
            .limit(10)
            .map(entry -> String.format(
                Locale.ROOT,
                "{用户ID=%d,交互数=%d,交互权重=[%s]}",
                entry.getKey(),
                entry.getValue().size(),
                formatSpotScoreEntries(entry.getValue(), 10)
            ))
            .collect(Collectors.joining(", "));
        log.info("离线矩阵更新：用户-景点交互矩阵样本=[{}]", samples);
    }

    private void logUserActivitySamples(Map<Long, Integer> userActivityCount) {
        String samples = userActivityCount.entrySet().stream()
            .sorted(Map.Entry.<Long, Integer>comparingByValue().reversed())
            .limit(10)
            .map(entry -> String.format(Locale.ROOT, "{用户ID=%d,交互景点数=%d}", entry.getKey(), entry.getValue()))
            .collect(Collectors.joining(", "));
        log.info("离线矩阵更新：用户活跃度样本=[{}]", samples);
    }

    private void logSpotSimilaritySummary(Long spotId, Map<Long, Double> topSimilarities) {
        String detail = topSimilarities.entrySet().stream()
            .limit(10)
            .map(entry -> String.format(
                Locale.ROOT,
                "{相似景点ID=%d,相似景点名称=%s,相似度=%.6f}",
                entry.getKey(),
                getSpotName(entry.getKey()),
                entry.getValue()
            ))
            .collect(Collectors.joining(", "));
        log.info(
            "离线矩阵更新：景点ID={}，景点名称={}，Top-K 相似邻居数={}，相似邻居明细=[{}]",
            spotId,
            getSpotName(spotId),
            topSimilarities.size(),
            detail
        );
    }

    private String formatSpotScoreEntries(Map<Long, Double> scoreMap, int limit) {
        if (scoreMap == null || scoreMap.isEmpty()) {
            return "";
        }
        return scoreMap.entrySet().stream()
            .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
            .limit(limit)
            .map(entry -> String.format(
                Locale.ROOT,
                "{景点ID=%d,景点名称=%s,分数=%.4f}",
                entry.getKey(),
                getSpotName(entry.getKey()),
                entry.getValue()
            ))
            .collect(Collectors.joining(", "));
    }

    private String formatSpotIdList(List<Long> spotIds, int limit) {
        if (spotIds == null || spotIds.isEmpty()) {
            return "";
        }
        return spotIds.stream()
            .limit(limit)
            .map(spotId -> String.format(Locale.ROOT, "{景点ID=%d,景点名称=%s}", spotId, getSpotName(spotId)))
            .collect(Collectors.joining(", "));
    }

    private String getSpotName(Long spotId) {
        if (spotId == null) {
            return "未知景点";
        }
        Spot spot = spotMapper.selectById(spotId);
        return spot == null || spot.getName() == null ? "未知景点" : spot.getName();
    }

    private Map<Long, Double> orderScoresByIds(List<Long> orderedIds, Map<Long, Double> scoreMap) {
        if (orderedIds == null || orderedIds.isEmpty() || scoreMap == null || scoreMap.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, Double> orderedScores = new LinkedHashMap<>();
        for (Long spotId : orderedIds) {
            Double score = scoreMap.get(spotId);
            if (score != null) {
                orderedScores.put(spotId, score);
            }
        }
        return orderedScores;
    }

    private Map<Long, Double> castScoreMap(Map<?, ?> rawMap) {
        Map<Long, Double> scoreMap = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
            Long spotId = castToLong(entry.getKey());
            Double score = castToDouble(entry.getValue());
            if (spotId != null && score != null) {
                scoreMap.put(spotId, score);
            }
        }
        return scoreMap;
    }

    // ==================== 配置管理与状态查询 ====================

    @Override
    public RecommendationConfigBundleDTO getConfig() {
        return RecommendationConfigBundleDTO.fromLegacy(recommendationCacheService.loadLegacyConfig());
    }

    @Override
    public void updateConfig(RecommendationConfigBundleDTO config) {
        LegacyRecommendationConfigDTO legacyConfig = (config == null
            ? RecommendationConfigBundleDTO.defaultConfig()
            : config).toLegacy();
        recommendationCacheService.saveLegacyConfig(legacyConfig);
        log.info("推荐算法配置已更新 {}", config);
    }

    @Override
    @SuppressWarnings("unchecked")
    public RecommendationStatusDTO getStatus() {
        RecommendationStatusDTO status = new RecommendationStatusDTO();
        status.setComputing(computing.get());

        Object cached = recommendationCacheService.getStatus();
        if (cached instanceof Map<?, ?> map) {
            status.setLastUpdateTime(map.get("lastUpdateTime") != null ? map.get("lastUpdateTime").toString() : null);
            status.setTotalUsers(map.get("totalUsers") instanceof Number n ? n.intValue() : null);
            status.setTotalSpots(map.get("totalSpots") instanceof Number n ? n.intValue() : null);
        }

        return status;
    }

    @Override
    public SimilarityPreviewResponse previewSimilarityNeighbors(Long spotId, Integer limit) {
        int safeLimit = limit == null || limit <= 0 ? 10 : limit;
        Map<Long, Double> similarities = getSimilarSpots(spotId);
        Map<Long, String> categoryMap = getCategoryMap();
        Map<Long, String> regionMap = getRegionMap();
        List<Long> neighborIds = similarities.keySet().stream().limit(safeLimit).collect(Collectors.toList());
        Map<Long, Spot> spotMap = spotMapper.selectBatchIds(neighborIds).stream()
            .collect(Collectors.toMap(Spot::getId, spot -> spot));

        SimilarityPreviewResponse response = new SimilarityPreviewResponse();
        response.setSpotId(spotId);
        response.setSpotName(getSpotName(spotId));
        response.setTotalNeighbors(similarities.size());
        response.setLastUpdateTime(getStatus().getLastUpdateTime());
        response.setNeighbors(neighborIds.stream().map(id -> {
            SimilarityPreviewResponse.NeighborItem item = new SimilarityPreviewResponse.NeighborItem();
            Spot spot = spotMap.get(id);
            item.setSpotId(id);
            item.setSpotName(spot == null ? "未知景点" : spot.getName());
            item.setCategoryName(spot == null ? null : categoryMap.get(spot.getCategoryId()));
            item.setRegionName(spot == null ? null : regionMap.get(spot.getRegionId()));
            item.setSimilarity(similarities.get(id));
            return item;
        }).collect(Collectors.toList()));
        return response;
    }
}



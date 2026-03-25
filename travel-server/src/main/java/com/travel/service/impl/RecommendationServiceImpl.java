package com.travel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.travel.dto.home.HotSpotResponse;
import com.travel.dto.recommendation.RecommendationConfigDTO;
import com.travel.dto.recommendation.RecommendationResponse;
import com.travel.dto.recommendation.RecommendationStatusDTO;
import com.travel.entity.*;
import com.travel.enums.OrderStatus;
import com.travel.mapper.*;
import com.travel.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
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
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String SIMILARITY_KEY = "recommendation:similarity:";
    private static final String USER_REC_KEY = "recommendation:user:";
    private static final String CONFIG_KEY = "recommendation:config";
    private static final String STATUS_KEY = "recommendation:status";

    private final AtomicBoolean computing = new AtomicBoolean(false);

    // ==================== 动态配置读取 ====================

    /**
     * 从 Redis 获取算法配置，不存在时返回默认配置
     */
    private RecommendationConfigDTO loadConfig() {
        Object cached = redisTemplate.opsForValue().get(CONFIG_KEY);
        if (cached instanceof RecommendationConfigDTO config) {
            return config;
        }
        if (cached instanceof Map) {
            // Jackson 反序列化可能生成 LinkedHashMap，手动转换
            return mapToConfig(cached);
        }
        return RecommendationConfigDTO.defaultConfig();
    }

    @SuppressWarnings("unchecked")
    private RecommendationConfigDTO mapToConfig(Object obj) {
        try {
            Map<String, Object> map = (Map<String, Object>) obj;
            RecommendationConfigDTO config = new RecommendationConfigDTO();
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
            log.warn("Failed to parse config from Redis, using default", e);
            return RecommendationConfigDTO.defaultConfig();
        }
    }

    private double toDouble(Object v) {
        return v instanceof Number n ? n.doubleValue() : Double.parseDouble(v.toString());
    }

    private int toInt(Object v) {
        return v instanceof Number n ? n.intValue() : Integer.parseInt(v.toString());
    }

    @Override
    public RecommendationResponse getRecommendations(Long userId, Integer limit) {
        if (limit == null || limit <= 0) limit = 10;

        // 检查缓存
        String cacheKey = USER_REC_KEY + userId;
        Object cached = redisTemplate.opsForValue().get(cacheKey);

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
        
        // 清除缓存
        String cacheKey = USER_REC_KEY + userId;
        redisTemplate.delete(cacheKey);
        
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
        redisTemplate.delete(USER_REC_KEY + userId);
    }

    private RecommendationResponse computeRecommendations(Long userId, Integer limit) {
        return computeRecommendations(userId, limit, false, false);
    }

    private RecommendationResponse computeRecommendations(Long userId, Integer limit, boolean refresh) {
        return computeRecommendations(userId, limit, refresh, false);
    }

    private RecommendationResponse computeRecommendations(Long userId, Integer limit, boolean refresh, boolean debug) {
        RecommendationConfigDTO config = loadConfig();

        log.info(
            "开始计算推荐结果：用户ID={}，请求数量={}，是否刷新={}，是否调试={}，协同过滤最少交互数={}，候选扩容倍数={}",
            userId,
            limit,
            refresh,
            debug,
            config.getMinInteractionsForCF(),
            getCandidateExpandFactor(config)
        );

        // ============ 改进：基于总交互景点数判断冷启动（而非仅评分数） ============
        Map<Long, Double> userInteractions = buildUserInteractionWeights(userId, config);
        logUserInteractionWeights(userId, userInteractions, debug);

        if (userInteractions.size() < config.getMinInteractionsForCF()) {
            log.info(
                "触发冷启动：用户ID={}，交互景点数={}，阈值={}，原因=交互景点数不足",
                userId,
                userInteractions.size(),
                config.getMinInteractionsForCF()
            );
            return handleColdStart(userId, limit, refresh, debug);
        }

        // 基于 ItemCF 计算推荐
        int candidateLimit = limit * getCandidateExpandFactor(config);
        Map<Long, Double> recommendedScores = computeItemCFRecommendationScores(userInteractions, candidateLimit);
        logScoreMap("ItemCF 原始候选分数", recommendedScores, debug);
        List<Long> recommendedIds = new ArrayList<>(recommendedScores.keySet());
        
        // 过滤已交互的景点
        List<Long> filteredIds = filterInteractedSpots(userId, recommendedIds);
        logFilteredRecommendations(userId, recommendedIds, filteredIds, debug);
        Map<Long, Double> filteredScores = orderScoresByIds(filteredIds, recommendedScores);
        logScoreMap("过滤已交互景点后的候选分数", filteredScores, debug);
        filteredScores = applyHeatRerank(filteredScores, config, debug);
        filteredIds = new ArrayList<>(filteredScores.keySet());
        
        if (filteredIds.isEmpty()) {
            log.info("个性化推荐结果为空，降级为冷启动：用户ID={}", userId);
            return handleColdStart(userId, limit, refresh, debug);
        }

        // 缓存结果
        if (refresh) {
            filteredIds = rotateRecommendations(filteredIds, limit);
            filteredScores = orderScoresByIds(filteredIds, filteredScores);
        }

        String cacheKey = USER_REC_KEY + userId;
        redisTemplate.opsForValue().set(cacheKey, filteredScores, config.getUserRecTTLMinutes(), TimeUnit.MINUTES);

        log.info(
            "推荐结果计算完成：用户ID={}，推荐类型=personalized，候选数={}，最终返回数={}，缓存时长={}分钟",
            userId,
            recommendedScores.size(),
            Math.min(filteredIds.size(), limit),
            config.getUserRecTTLMinutes()
        );

        return buildRecommendationResponse(filteredIds, filteredScores, limit, "personalized", false);
    }

    /**
     * 构建单个用户的交互权重 Map<spotId, weight>
     * 融合浏览、收藏、评分、订单四种行为，同一景点按加权求和聚合
     */
    private Map<Long, Double> buildUserInteractionWeights(Long userId, RecommendationConfigDTO config) {
        Map<Long, Double> weights = new HashMap<>();

        // 浏览（去重，取不同景点）
        List<UserSpotView> views = userSpotViewMapper.selectList(
            new LambdaQueryWrapper<UserSpotView>()
                .eq(UserSpotView::getUserId, userId)
                .select(UserSpotView::getSpotId, UserSpotView::getViewSource, UserSpotView::getViewDuration)
        );
        for (UserSpotView v : views) {
            mergeInteractionWeight(weights, v.getSpotId(), calculateViewWeight(v, config));
        }

        // 收藏
        List<UserSpotFavorite> favorites = userSpotFavoriteMapper.selectList(
            new LambdaQueryWrapper<UserSpotFavorite>()
                .eq(UserSpotFavorite::getUserId, userId)
                .eq(UserSpotFavorite::getIsDeleted, 0)
                .select(UserSpotFavorite::getSpotId)
        );
        for (UserSpotFavorite f : favorites) {
            mergeInteractionWeight(weights, f.getSpotId(), config.getWeightFavorite());
        }

        // 评分
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

        // 订单（已支付 & 已完成）
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
     * 冷启动处理：基于用户偏好或热门推荐
     */
    private RecommendationResponse handleColdStart(Long userId, Integer limit) {
        return handleColdStart(userId, limit, false);
    }

    /**
     * 冷启动处理：刷新时对返回结果做轮换，避免每次都是同一批
     */
    private RecommendationResponse handleColdStart(Long userId, Integer limit, boolean refresh) {
        return handleColdStart(userId, limit, refresh, false);
    }

    private RecommendationResponse handleColdStart(Long userId, Integer limit, boolean refresh, boolean debug) {
        RecommendationConfigDTO config = loadConfig();
        List<Long> categoryIds = getUserPreferenceCategoryIds(userId);

        // 如果用户设置了偏好标签，基于偏好推荐
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
            logColdStartResult(userId, "preference", categoryIds, spotIds, debug);
            return buildRecommendationResponse(spotIds, limit, "preference", false);
        }

        // 无偏好，返回热门并提示设置偏好
        HotSpotResponse hotSpots = getHotSpots(refresh ? Math.max(limit * getColdStartExpandFactor(config), limit) : limit);
        List<HotSpotResponse.SpotItem> hotSpotList = new ArrayList<>(hotSpots.getList());
        if (refresh) {
            rotateSpotItems(hotSpotList, limit);
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
     * 基于 ItemCF 计算推荐
     * 公式 P_uj = Σ_{i ∈ N(u) ∩ S(j,K)} w_ji × r_ui
     *
     * @param userInteractions 当前用户的交互权重 Map<spotId, weight>
     */
    /**
     * 刷新推荐时尝试跳过当前头部几个结果，让用户看到新的一批
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
     * 热门/偏好冷启动结果没有个性化排序，刷新时做一次轮换
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
        if (userInteractions.isEmpty()) {
            return Collections.emptyMap();
        }

        // 计算推荐分数
        Map<Long, Double> scores = new HashMap<>();
        
        for (Map.Entry<Long, Double> entry : userInteractions.entrySet()) {
            Long spotId = entry.getKey();
            Double rui = entry.getValue(); // 综合交互权重

            // 获取相似景点（已是 Top-K）
            Map<Long, Double> similarities = getSimilarSpots(spotId);
            
            for (Map.Entry<Long, Double> simEntry : similarities.entrySet()) {
                Long similarSpotId = simEntry.getKey();
                Double wji = simEntry.getValue(); // 相似度

                // 跳过用户已交互的景点
                if (userInteractions.containsKey(similarSpotId)) {
                    continue;
                }
                
                // 公式：P_uj += w_ji × r_ui
                scores.merge(similarSpotId, wji * rui, Double::sum);
            }
        }

        // 按分数排序返回
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

    private Map<Long, Double> applyHeatRerank(Map<Long, Double> scoreMap, RecommendationConfigDTO config) {
        return applyHeatRerank(scoreMap, config, false);
    }

    private Map<Long, Double> applyHeatRerank(Map<Long, Double> scoreMap, RecommendationConfigDTO config, boolean debug) {
        if (scoreMap == null || scoreMap.isEmpty()) {
            return Collections.emptyMap();
        }

        double rerankFactor = config.getHeatRerankFactor() == null ? 0.0 : config.getHeatRerankFactor();
        if (rerankFactor <= 0) {
            if (debug) {
                log.info("热度重排已跳过：原因=热度重排系数<=0，当前系数={}", rerankFactor);
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
                log.info("热度重排已跳过：原因=候选景点热度均为空或为0");
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
     * 获取相似景点（从 Redis 缓存）
     */
    @SuppressWarnings("unchecked")
    private Map<Long, Double> getSimilarSpots(Long spotId) {
        String key = SIMILARITY_KEY + spotId;
        Object cached = redisTemplate.opsForValue().get(key);
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
                log.warn("Failed to parse Redis key to Long: {}", stringValue);
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
                log.warn("Failed to parse Redis value to Double: {}", stringValue);
            }
        }
        return null;
    }

    /**
     * 过滤已交互的景点（已评分、已收藏、已下单未取消）
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
     * 更新物品相似度矩阵（离线计算）
     * 
     * 1. 融合浏览、收藏、评分、订单四种行为构建交互矩阵
     * 2. 使用 IUF 加权余弦相似度  公式 w_ij = Σ_{u ∈ N(i)∩N(j)} 1/log(1+|N(u)|) / (√|N(i)| × √|N(j)|)
     */
    @Override
    public void updateSimilarityMatrix() {
        if (!computing.compareAndSet(false, true)) {
            log.warn("相似度矩阵正在计算中，跳过本次请求");
            return;
        }

        try {
            RecommendationConfigDTO config = loadConfig();
            log.info("开始更新物品相似度矩阵...");
            Set<Long> activeSpotIds = getActiveSpotIds();
            if (activeSpotIds.isEmpty()) {
                log.info("无有效上架景点，跳过相似度计算");
                return;
            }

            // ============ 步骤1：构建全局用户-景点交互矩阵 ============
            // Map<userId, Map<spotId, weight>>
            Map<Long, Map<Long, Double>> userItemMatrix = new HashMap<>();
            Set<Long> allSpotIds = new HashSet<>();

            // 1a. 浏览数据
            List<UserSpotView> allViews = userSpotViewMapper.selectList(
                new LambdaQueryWrapper<UserSpotView>()
                    .select(UserSpotView::getUserId, UserSpotView::getSpotId, UserSpotView::getViewSource, UserSpotView::getViewDuration)
            );
            log.info("离线矩阵更新：读取浏览行为 {} 条", allViews.size());
            for (UserSpotView v : allViews) {
                if (!activeSpotIds.contains(v.getSpotId())) continue;
                mergeInteractionWeight(
                    userItemMatrix.computeIfAbsent(v.getUserId(), k -> new HashMap<>()),
                    v.getSpotId(),
                    calculateViewWeight(v, config)
                );
                allSpotIds.add(v.getSpotId());
            }

            // 1b. 收藏数据
            List<UserSpotFavorite> allFavorites = userSpotFavoriteMapper.selectList(
                new LambdaQueryWrapper<UserSpotFavorite>()
                    .eq(UserSpotFavorite::getIsDeleted, 0)
                    .select(UserSpotFavorite::getUserId, UserSpotFavorite::getSpotId)
            );
            log.info("离线矩阵更新：读取收藏行为 {} 条", allFavorites.size());
            for (UserSpotFavorite f : allFavorites) {
                if (!activeSpotIds.contains(f.getSpotId())) continue;
                mergeInteractionWeight(
                    userItemMatrix.computeIfAbsent(f.getUserId(), k -> new HashMap<>()),
                    f.getSpotId(),
                    config.getWeightFavorite()
                );
                allSpotIds.add(f.getSpotId());
            }

            // 1c. 评分数据
            List<Review> allRatings = reviewMapper.selectList(
                new LambdaQueryWrapper<Review>()
                    .eq(Review::getIsDeleted, 0)
                    .select(Review::getUserId, Review::getSpotId, Review::getScore)
            );
            log.info("离线矩阵更新：读取评分行为 {} 条", allRatings.size());
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

            // 1d. 订单数据（已支付 + 已完成）
            List<Order> allOrders = orderMapper.selectList(
                new LambdaQueryWrapper<Order>()
                    .eq(Order::getIsDeleted, 0)
                    .in(Order::getStatus, OrderStatus.PAID.getCode(), OrderStatus.COMPLETED.getCode())
                    .select(Order::getUserId, Order::getSpotId, Order::getStatus)
            );
            log.info("离线矩阵更新：读取有效订单行为 {} 条", allOrders.size());
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
                log.info("无交互数据，跳过相似度计算");
                return;
            }

            log.info("交互矩阵构建完成：{} 个用户，{} 个景点", userItemMatrix.size(), allSpotIds.size());
            logUserItemMatrixSamples(userItemMatrix);

            // ============ 步骤2：预计算每个用户的交互景点数 |N(u)|（用于 IUF） ============
            Map<Long, Integer> userActivityCount = new HashMap<>();
            for (Map.Entry<Long, Map<Long, Double>> entry : userItemMatrix.entrySet()) {
                userActivityCount.put(entry.getKey(), entry.getValue().size());
            }
            logUserActivitySamples(userActivityCount);

            // ============ 步骤3：构建物品到用户的倒排索引 ============
            // Map<spotId, Set<userId>>  即 N(i)
            Map<Long, Set<Long>> spotUserSets = new HashMap<>();
            for (Map.Entry<Long, Map<Long, Double>> entry : userItemMatrix.entrySet()) {
                Long userId = entry.getKey();
                for (Long spotId : entry.getValue().keySet()) {
                    spotUserSets.computeIfAbsent(spotId, k -> new HashSet<>()).add(userId);
                }
            }

            // ============ 步骤4：计算 IUF 加权相似度 ============
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

                    // 计算 IUF 加权相似度
                    double similarity = computeIUFSimilarity(usersI, usersJ, userActivityCount);

                    if (similarity > 0) {
                        similarities.put(spotJ, similarity);
                    }
                }

                // 只保留 Top-K 相似物品
                Map<Long, Double> topSimilarities = similarities.entrySet().stream()
                    .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                    .limit(topK)
                    .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                    ));

                // 存入 Redis
                String key = SIMILARITY_KEY + spotI;
                redisTemplate.opsForValue().set(key, java.util.Objects.requireNonNull(topSimilarities), simTTL, TimeUnit.HOURS);
                logSpotSimilaritySummary(spotI, topSimilarities);
            }

            // ============ 保存状态信息 ============
            Map<String, Object> statusMap = new HashMap<>();
            statusMap.put("lastUpdateTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            statusMap.put("totalUsers", userItemMatrix.size());
            statusMap.put("totalSpots", allSpotIds.size());
            redisTemplate.opsForValue().set(STATUS_KEY, statusMap);

            log.info(
                "物品相似度矩阵更新完成：共处理景点 {} 个，用户 {} 个，相似度缓存时长 {} 小时，Top-K 邻居数 {}",
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
     * IUF 加权余弦相似度
     * 公式 w_ij = Σ_{u ∈ N(i)∩N(j)} 1/log(1+|N(u)|) / (√|N(i)| × √|N(j)|)
     *
     * @param usersI 交互过景点 i 的用户集合 N(i)
     * @param usersJ 交互过景点 j 的用户集合 N(j)
     * @param userActivityCount 每个用户的交互景点总数 |N(u)|
     */
    private double computeIUFSimilarity(Set<Long> usersI, Set<Long> usersJ,
                                         Map<Long, Integer> userActivityCount) {
        // 找较小的集合来遍历（优化性能）
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

    private double calculateViewWeight(UserSpotView view, RecommendationConfigDTO config) {
        double baseWeight = config.getWeightView() == null ? 0.5 : config.getWeightView();
        return baseWeight * getViewSourceFactor(view.getViewSource(), config) * getViewDurationFactor(view.getViewDuration(), config);
    }

    private void mergeInteractionWeight(Map<Long, Double> weights, Long spotId, Double weight) {
        if (weights == null || spotId == null || weight == null || weight <= 0) {
            return;
        }
        weights.merge(spotId, weight, Double::sum);
    }

    private double getViewSourceFactor(String source, RecommendationConfigDTO config) {
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

    private double getViewDurationFactor(Integer duration, RecommendationConfigDTO config) {
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

    private int getCandidateExpandFactor(RecommendationConfigDTO config) {
        return Math.max(defaultInt(config.getCandidateExpandFactor(), 2), 1);
    }

    private int getColdStartExpandFactor(RecommendationConfigDTO config) {
        return Math.max(defaultInt(config.getColdStartExpandFactor(), 3), 1);
    }

    private RecommendationResponse buildRecommendationResponse(List<Long> spotIds, Integer limit, String type, Boolean needPreference) {
        return buildRecommendationResponse(spotIds, null, limit, type, needPreference);
    }

    private RecommendationResponse buildRecommendationResponse(List<Long> spotIds, Map<Long, Double> scoreMap, Integer limit, String type, Boolean needPreference) {
        List<Long> limitedIds = spotIds.stream().limit(limit).collect(Collectors.toList());
        
        if (limitedIds.isEmpty()) {
            RecommendationResponse response = new RecommendationResponse();
            response.setType(type);
            response.setList(Collections.emptyList());
            response.setNeedPreference(needPreference);
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
            "推荐调试预览结果：用户ID={}，是否刷新={}，推荐类型={}，是否需要偏好引导={}，结果项明细=[{}]",
            userId,
            refresh,
            response.getType(),
            response.getNeedPreference(),
            items
        );
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
        String reason = "preference".equals(type) ? "存在用户偏好，按偏好分类推荐" : "无足够个性化信号，回退热门推荐";
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
    public RecommendationConfigDTO getConfig() {
        return loadConfig();
    }

    @Override
    public void updateConfig(RecommendationConfigDTO config) {
        redisTemplate.opsForValue().set(CONFIG_KEY, config);
        log.info("推荐算法配置已更新: {}", config);
    }

    @Override
    @SuppressWarnings("unchecked")
    public RecommendationStatusDTO getStatus() {
        RecommendationStatusDTO status = new RecommendationStatusDTO();
        status.setComputing(computing.get());

        Object cached = redisTemplate.opsForValue().get(STATUS_KEY);
        if (cached instanceof Map<?, ?> map) {
            status.setLastUpdateTime(map.get("lastUpdateTime") != null ? map.get("lastUpdateTime").toString() : null);
            status.setTotalUsers(map.get("totalUsers") instanceof Number n ? n.intValue() : null);
            status.setTotalSpots(map.get("totalSpots") instanceof Number n ? n.intValue() : null);
        }

        return status;
    }
}

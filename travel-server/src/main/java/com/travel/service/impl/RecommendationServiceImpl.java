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
            if (map.containsKey("minInteractionsForCF")) config.setMinInteractionsForCF(toInt(map.get("minInteractionsForCF")));
            if (map.containsKey("topKNeighbors")) config.setTopKNeighbors(toInt(map.get("topKNeighbors")));
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
        @SuppressWarnings("unchecked")
        List<Long> cachedIds = (List<Long>) redisTemplate.opsForValue().get(cacheKey);
        
        if (cachedIds != null && !cachedIds.isEmpty()) {
            return buildRecommendationResponse(cachedIds, limit, "personalized", false);
        }

        return computeRecommendations(userId, limit, false);
    }

    @Override
    public RecommendationResponse refreshRecommendations(Long userId, Integer limit) {
        if (limit == null || limit <= 0) limit = 10;
        
        // 清除缓存
        String cacheKey = USER_REC_KEY + userId;
        redisTemplate.delete(cacheKey);
        
        return computeRecommendations(userId, limit, true);
    }

    @Override
    public void invalidateUserRecommendationCache(Long userId) {
        if (userId == null) {
            return;
        }
        redisTemplate.delete(USER_REC_KEY + userId);
    }

    private RecommendationResponse computeRecommendations(Long userId, Integer limit) {
        return computeRecommendations(userId, limit, false);
    }

    private RecommendationResponse computeRecommendations(Long userId, Integer limit, boolean refresh) {
        RecommendationConfigDTO config = loadConfig();

        // ============ 改进：基于总交互景点数判断冷启动（而非仅评分数） ============
        Map<Long, Double> userInteractions = buildUserInteractionWeights(userId, config);

        if (userInteractions.size() < config.getMinInteractionsForCF()) {
            return handleColdStart(userId, limit, refresh);
        }

        // 基于 ItemCF 计算推荐
        List<Long> recommendedIds = computeItemCFRecommendations(userId, userInteractions, limit * 2);
        
        // 过滤已交互的景点
        List<Long> filteredIds = filterInteractedSpots(userId, recommendedIds);
        
        if (filteredIds.isEmpty()) {
            return handleColdStart(userId, limit, refresh);
        }

        // 缓存结果
        if (refresh) {
            filteredIds = rotateRecommendations(filteredIds, limit);
        }

        String cacheKey = USER_REC_KEY + userId;
        redisTemplate.opsForValue().set(cacheKey, filteredIds, config.getUserRecTTLMinutes(), TimeUnit.MINUTES);

        return buildRecommendationResponse(filteredIds, limit, "personalized", false);
    }

    /**
     * 构建单个用户的交互权重 Map<spotId, weight>
     * 融合浏览、收藏、评分、订单四种行为，同一景点取最大权重
     */
    private Map<Long, Double> buildUserInteractionWeights(Long userId, RecommendationConfigDTO config) {
        Map<Long, Double> weights = new HashMap<>();

        // 浏览（去重，取不同景点）
        List<UserSpotView> views = userSpotViewMapper.selectList(
            new LambdaQueryWrapper<UserSpotView>()
                .eq(UserSpotView::getUserId, userId)
                .select(UserSpotView::getSpotId)
        );
        for (UserSpotView v : views) {
            weights.merge(v.getSpotId(), config.getWeightView(), Math::max);
        }

        // 收藏
        List<UserSpotFavorite> favorites = userSpotFavoriteMapper.selectList(
            new LambdaQueryWrapper<UserSpotFavorite>()
                .eq(UserSpotFavorite::getUserId, userId)
                .eq(UserSpotFavorite::getIsDeleted, 0)
                .select(UserSpotFavorite::getSpotId)
        );
        for (UserSpotFavorite f : favorites) {
            weights.merge(f.getSpotId(), config.getWeightFavorite(), Math::max);
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
            weights.merge(r.getSpotId(), w, Math::max);
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
            weights.merge(o.getSpotId(), w, Math::max);
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
     * 鍐峰惎鍔ㄥ鐞嗭細鍒锋柊鏃跺硅繑鍥炵粨鏋滃仛杞崲锛岄伩鍏嶆瘡娆￠兘鏄悓涓€鎵?     */
    private RecommendationResponse handleColdStart(Long userId, Integer limit, boolean refresh) {
        List<Long> categoryIds = getUserPreferenceCategoryIds(userId);

        // 如果用户设置了偏好标签，基于偏好推荐
        if (!categoryIds.isEmpty()) {
            List<Spot> spots = spotMapper.selectList(
                new LambdaQueryWrapper<Spot>()
                    .eq(Spot::getIsPublished, 1)
                    .in(Spot::getCategoryId, categoryIds)
                    .eq(Spot::getIsDeleted, 0)
                    .orderByDesc(Spot::getHeatScore)
                    .last("LIMIT " + (refresh ? Math.max(limit * 3, limit) : limit))
            );

            List<Long> spotIds = spots.stream().map(Spot::getId).collect(Collectors.toList());
            if (refresh) {
                spotIds = rotateRecommendations(spotIds, limit);
            }
            return buildRecommendationResponse(spotIds, limit, "preference", false);
        }

        // 无偏好，返回热门并提示设置偏好
        HotSpotResponse hotSpots = getHotSpots(refresh ? Math.max(limit * 3, limit) : limit);
        List<HotSpotResponse.SpotItem> hotSpotList = new ArrayList<>(hotSpots.getList());
        if (refresh) {
            rotateSpotItems(hotSpotList, limit);
        }
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

        Map<Long, String> categoryMap = getCategoryMap();
        Map<String, Long> nameToIdMap = categoryMap.entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey, (left, right) -> left));

        return preferences.stream()
            .map(UserPreference::getTag)
            .map(tag -> parsePreferenceCategoryId(tag, nameToIdMap))
            .filter(Objects::nonNull)
            .distinct()
            .collect(Collectors.toList());
    }

    private Long parsePreferenceCategoryId(String tag, Map<String, Long> nameToIdMap) {
        if (tag == null || tag.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(tag.trim());
        } catch (NumberFormatException ignored) {
            return nameToIdMap.get(tag.trim());
        }
    }

    /**
     * 基于 ItemCF 计算推荐（论文公式 2-3）
     * P_uj = Σ_{i ∈ N(u) ∩ S(j,K)} w_ji × r_ui
     *
     * @param userInteractions 当前用户的交互权重 Map<spotId, weight>
     */
    /**
     * 鍒锋柊鎺ㄨ崘鏃跺皾璇曡烦杩囧綋鍓嶉《閮ㄥ嚑涓粨鏋滐紝璁╃敤鎴风湅鍒版柊鐨勪竴鎵?     */
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
     * 鐑棬/鍋忓ソ鍐峰惎鍔ㄧ粨鏋滄病鏈夌壒鍒殑涓€у寲鎺掑簭锛屽埛鏂版椂鍋氫竴娆¤疆鎹?     */
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

    private List<Long> computeItemCFRecommendations(Long userId, Map<Long, Double> userInteractions, Integer limit) {
        if (userInteractions.isEmpty()) {
            return Collections.emptyList();
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
                
                // 论文公式 (2-3)：P_uj += w_ji × r_ui
                scores.merge(similarSpotId, wji * rui, Double::sum);
            }
        }

        // 按分数排序返回
        return scores.entrySet().stream()
            .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
            .limit(limit)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
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
     * 改进点：
     * 1. 融合浏览、收藏、评分、订单四种行为构建交互矩阵
     * 2. 使用 IUF 加权余弦相似度（论文公式 2-2）
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

            // ============ 步骤1：构建全局用户-景点交互矩阵 ============
            // Map<userId, Map<spotId, weight>>
            Map<Long, Map<Long, Double>> userItemMatrix = new HashMap<>();
            Set<Long> allSpotIds = new HashSet<>();

            // 1a. 浏览数据
            List<UserSpotView> allViews = userSpotViewMapper.selectList(
                new LambdaQueryWrapper<UserSpotView>()
                    .select(UserSpotView::getUserId, UserSpotView::getSpotId)
            );
            for (UserSpotView v : allViews) {
                userItemMatrix.computeIfAbsent(v.getUserId(), k -> new HashMap<>())
                    .merge(v.getSpotId(), config.getWeightView(), Math::max);
                allSpotIds.add(v.getSpotId());
            }

            // 1b. 收藏数据
            List<UserSpotFavorite> allFavorites = userSpotFavoriteMapper.selectList(
                new LambdaQueryWrapper<UserSpotFavorite>()
                    .eq(UserSpotFavorite::getIsDeleted, 0)
                    .select(UserSpotFavorite::getUserId, UserSpotFavorite::getSpotId)
            );
            for (UserSpotFavorite f : allFavorites) {
                userItemMatrix.computeIfAbsent(f.getUserId(), k -> new HashMap<>())
                    .merge(f.getSpotId(), config.getWeightFavorite(), Math::max);
                allSpotIds.add(f.getSpotId());
            }

            // 1c. 评分数据
            List<Review> allRatings = reviewMapper.selectList(
                new LambdaQueryWrapper<Review>()
                    .eq(Review::getIsDeleted, 0)
                    .select(Review::getUserId, Review::getSpotId, Review::getScore)
            );
            for (Review r : allRatings) {
                double w = r.getScore() * config.getWeightReviewFactor();
                userItemMatrix.computeIfAbsent(r.getUserId(), k -> new HashMap<>())
                    .merge(r.getSpotId(), w, Math::max);
                allSpotIds.add(r.getSpotId());
            }

            // 1d. 订单数据（已支付 + 已完成）
            List<Order> allOrders = orderMapper.selectList(
                new LambdaQueryWrapper<Order>()
                    .eq(Order::getIsDeleted, 0)
                    .in(Order::getStatus, OrderStatus.PAID.getCode(), OrderStatus.COMPLETED.getCode())
                    .select(Order::getUserId, Order::getSpotId, Order::getStatus)
            );
            for (Order o : allOrders) {
                double w = o.getStatus() == OrderStatus.COMPLETED.getCode() ? config.getWeightOrderCompleted() : config.getWeightOrderPaid();
                userItemMatrix.computeIfAbsent(o.getUserId(), k -> new HashMap<>())
                    .merge(o.getSpotId(), w, Math::max);
                allSpotIds.add(o.getSpotId());
            }

            if (userItemMatrix.isEmpty()) {
                log.info("无交互数据，跳过相似度计算");
                return;
            }

            log.info("交互矩阵构建完成：{} 个用户，{} 个景点", userItemMatrix.size(), allSpotIds.size());

            // ============ 步骤2：预计算每个用户的交互景点数 |N(u)|（用于 IUF） ============
            Map<Long, Integer> userActivityCount = new HashMap<>();
            for (Map.Entry<Long, Map<Long, Double>> entry : userItemMatrix.entrySet()) {
                userActivityCount.put(entry.getKey(), entry.getValue().size());
            }

            // ============ 步骤3：构建物品到用户的倒排索引 ============
            // Map<spotId, Set<userId>>  即 N(i)
            Map<Long, Set<Long>> spotUserSets = new HashMap<>();
            for (Map.Entry<Long, Map<Long, Double>> entry : userItemMatrix.entrySet()) {
                Long userId = entry.getKey();
                for (Long spotId : entry.getValue().keySet()) {
                    spotUserSets.computeIfAbsent(spotId, k -> new HashSet<>()).add(userId);
                }
            }

            // ============ 步骤4：计算 IUF 加权相似度（论文公式 2-2） ============
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
            }

            // ============ 保存状态信息 ============
            Map<String, Object> statusMap = new HashMap<>();
            statusMap.put("lastUpdateTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            statusMap.put("totalUsers", userItemMatrix.size());
            statusMap.put("totalSpots", allSpotIds.size());
            redisTemplate.opsForValue().set(STATUS_KEY, statusMap);

            log.info("物品相似度矩阵更新完成，共处理 {} 个景点", allSpotIds.size());
        } finally {
            computing.set(false);
        }
    }

    /**
     * IUF 加权余弦相似度（论文公式 2-2）
     *
     * w_ij = Σ_{u ∈ N(i)∩N(j)} 1/log(1+|N(u)|) / (√|N(i)| × √|N(j)|)
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

    private RecommendationResponse buildRecommendationResponse(List<Long> spotIds, Integer limit, String type, Boolean needPreference) {
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

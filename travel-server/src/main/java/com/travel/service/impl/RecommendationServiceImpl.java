package com.travel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.travel.dto.home.HotSpotResponse;
import com.travel.dto.recommendation.RecommendationResponse;
import com.travel.entity.*;
import com.travel.mapper.*;
import com.travel.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationServiceImpl implements RecommendationService {

    private final SpotMapper spotMapper;
    private final RatingMapper ratingMapper;
    private final FavoriteMapper favoriteMapper;
    private final OrderMapper orderMapper;
    private final SpotCategoryMapper categoryMapper;
    private final RegionMapper regionMapper;
    private final UserMapper userMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String SIMILARITY_KEY = "recommendation:similarity:";
    private static final String USER_REC_KEY = "recommendation:user:";
    private static final int MIN_RATINGS_FOR_CF = 3; // 协同过滤最少评分数

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

        return computeRecommendations(userId, limit);
    }

    @Override
    public RecommendationResponse refreshRecommendations(Long userId, Integer limit) {
        if (limit == null || limit <= 0) limit = 10;
        
        // 清除缓存
        String cacheKey = USER_REC_KEY + userId;
        redisTemplate.delete(cacheKey);
        
        return computeRecommendations(userId, limit);
    }

    private RecommendationResponse computeRecommendations(Long userId, Integer limit) {
        // 获取用户评分数量
        Long ratingCount = ratingMapper.selectCount(
            new LambdaQueryWrapper<Rating>()
                .eq(Rating::getUserId, userId)
                .eq(Rating::getIsDeleted, 0)
        );

        // 冷启动：评分不足
        if (ratingCount < MIN_RATINGS_FOR_CF) {
            return handleColdStart(userId, limit);
        }

        // 基于 ItemCF 计算推荐
        List<Long> recommendedIds = computeItemCFRecommendations(userId, limit * 2);
        
        // 过滤已交互的景点
        List<Long> filteredIds = filterInteractedSpots(userId, recommendedIds);
        
        if (filteredIds.isEmpty()) {
            return handleColdStart(userId, limit);
        }

        // 缓存结果
        String cacheKey = USER_REC_KEY + userId;
        redisTemplate.opsForValue().set(cacheKey, filteredIds, 1, TimeUnit.HOURS);

        return buildRecommendationResponse(filteredIds, limit, "personalized", false);
    }


    /**
     * 冷启动处理：基于用户偏好或热门推荐
     */
    private RecommendationResponse handleColdStart(Long userId, Integer limit) {
        User user = userMapper.selectById(userId);
        String preferences = user != null ? user.getPreferences() : null;

        // 如果用户设置了偏好标签，基于偏好推荐
        if (preferences != null && !preferences.isEmpty()) {
            List<Long> categoryIds = Arrays.stream(preferences.split(","))
                .map(Long::parseLong)
                .collect(Collectors.toList());
            
            List<Spot> spots = spotMapper.selectList(
                new LambdaQueryWrapper<Spot>()
                    .eq(Spot::getPublished, 1)
                    .in(Spot::getCategoryId, categoryIds)
                    .eq(Spot::getIsDeleted, 0)
                    .orderByDesc(Spot::getHeatScore)
                    .last("LIMIT " + limit)
            );

            List<Long> spotIds = spots.stream().map(Spot::getId).collect(Collectors.toList());
            return buildRecommendationResponse(spotIds, limit, "preference", false);
        }

        // 无偏好，返回热门并提示设置偏好
        HotSpotResponse hotSpots = getHotSpots(limit);
        RecommendationResponse response = new RecommendationResponse();
        response.setType("hot");
        response.setNeedPreference(true);
        response.setList(hotSpots.getList().stream()
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

    /**
     * 基于 ItemCF 计算推荐
     */
    private List<Long> computeItemCFRecommendations(Long userId, Integer limit) {
        // 获取用户评分过的景点
        List<Rating> userRatings = ratingMapper.selectList(
            new LambdaQueryWrapper<Rating>()
                .eq(Rating::getUserId, userId)
                .eq(Rating::getIsDeleted, 0)
        );

        if (userRatings.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, Integer> userRatingMap = userRatings.stream()
            .collect(Collectors.toMap(Rating::getSpotId, Rating::getScore));

        // 计算推荐分数
        Map<Long, Double> scores = new HashMap<>();
        
        for (Map.Entry<Long, Integer> entry : userRatingMap.entrySet()) {
            Long spotId = entry.getKey();
            Integer rating = entry.getValue();
            
            // 获取相似景点
            Map<Long, Double> similarities = getSimilarSpots(spotId);
            
            for (Map.Entry<Long, Double> simEntry : similarities.entrySet()) {
                Long similarSpotId = simEntry.getKey();
                Double similarity = simEntry.getValue();
                
                // 跳过用户已评分的景点
                if (userRatingMap.containsKey(similarSpotId)) {
                    continue;
                }
                
                // 累加推荐分数 = 相似度 * 用户评分
                scores.merge(similarSpotId, similarity * rating, Double::sum);
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
        Map<Long, Double> similarities = (Map<Long, Double>) redisTemplate.opsForValue().get(key);
        return similarities != null ? similarities : Collections.emptyMap();
    }

    /**
     * 过滤已交互的景点（已评分、已收藏、已下单未取消）
     */
    private List<Long> filterInteractedSpots(Long userId, List<Long> spotIds) {
        if (spotIds.isEmpty()) return spotIds;

        // 已评分
        Set<Long> ratedIds = ratingMapper.selectList(
            new LambdaQueryWrapper<Rating>()
                .eq(Rating::getUserId, userId)
                .eq(Rating::getIsDeleted, 0)
        ).stream().map(Rating::getSpotId).collect(Collectors.toSet());

        // 已收藏
        Set<Long> favoriteIds = favoriteMapper.selectList(
            new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, userId)
                .eq(Favorite::getIsDeleted, 0)
        ).stream().map(Favorite::getSpotId).collect(Collectors.toSet());

        // 已下单（不含已取消）
        Set<Long> orderedIds = orderMapper.selectList(
            new LambdaQueryWrapper<Order>()
                .eq(Order::getUserId, userId)
                .eq(Order::getIsDeleted, 0)
                .ne(Order::getStatus, Order.STATUS_CANCELLED)
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
                .eq(Spot::getPublished, 1)
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
            item.setCoverImage(spot.getCoverImage());
            item.setPrice(spot.getPrice());
            item.setAvgRating(spot.getAvgRating());
            item.setHeatScore(spot.getHeatScore());
            item.setCategoryName(categoryMap.get(spot.getCategoryId()));
            return item;
        }).collect(Collectors.toList()));

        return response;
    }

    @Override
    public void updateSimilarityMatrix() {
        log.info("开始更新物品相似度矩阵...");

        // 获取所有评分数据
        List<Rating> allRatings = ratingMapper.selectList(
            new LambdaQueryWrapper<Rating>().eq(Rating::getIsDeleted, 0)
        );
        
        if (allRatings.isEmpty()) {
            log.info("无评分数据，跳过相似度计算");
            return;
        }

        // 构建用户-物品评分矩阵
        Map<Long, Map<Long, Integer>> userItemMatrix = new HashMap<>();
        Set<Long> allSpotIds = new HashSet<>();
        
        for (Rating rating : allRatings) {
            userItemMatrix
                .computeIfAbsent(rating.getUserId(), k -> new HashMap<>())
                .put(rating.getSpotId(), rating.getScore());
            allSpotIds.add(rating.getSpotId());
        }

        // 计算物品相似度（余弦相似度）
        List<Long> spotIdList = new ArrayList<>(allSpotIds);
        
        for (int i = 0; i < spotIdList.size(); i++) {
            Long spotI = spotIdList.get(i);
            Map<Long, Double> similarities = new HashMap<>();
            
            for (int j = 0; j < spotIdList.size(); j++) {
                if (i == j) continue;
                
                Long spotJ = spotIdList.get(j);
                double similarity = computeCosineSimilarity(spotI, spotJ, userItemMatrix);
                
                if (similarity > 0) {
                    similarities.put(spotJ, similarity);
                }
            }

            // 只保留 Top-N 相似物品
            Map<Long, Double> topSimilarities = similarities.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(20)
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    Map.Entry::getValue,
                    (e1, e2) -> e1,
                    LinkedHashMap::new
                ));

            // 存入 Redis
            String key = SIMILARITY_KEY + spotI;
            redisTemplate.opsForValue().set(key, topSimilarities, 24, TimeUnit.HOURS);
        }

        log.info("物品相似度矩阵更新完成，共处理 {} 个景点", allSpotIds.size());
    }

    /**
     * 计算余弦相似度
     */
    private double computeCosineSimilarity(Long spotI, Long spotJ, Map<Long, Map<Long, Integer>> userItemMatrix) {
        double dotProduct = 0;
        double normI = 0;
        double normJ = 0;

        for (Map<Long, Integer> userRatings : userItemMatrix.values()) {
            Integer ratingI = userRatings.get(spotI);
            Integer ratingJ = userRatings.get(spotJ);

            if (ratingI != null) {
                normI += ratingI * ratingI;
                if (ratingJ != null) {
                    dotProduct += ratingI * ratingJ;
                }
            }
            if (ratingJ != null) {
                normJ += ratingJ * ratingJ;
            }
        }

        if (normI == 0 || normJ == 0) return 0;
        return dotProduct / (Math.sqrt(normI) * Math.sqrt(normJ));
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
            .filter(spot -> spot != null && spot.getIsDeleted() == 0 && spot.getPublished() == 1)
            .map(spot -> {
                RecommendationResponse.SpotItem item = new RecommendationResponse.SpotItem();
                item.setId(spot.getId());
                item.setName(spot.getName());
                item.setCoverImage(spot.getCoverImage());
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
        return regionMapper.selectList(new LambdaQueryWrapper<Region>().eq(Region::getIsDeleted, 0)).stream()
            .collect(Collectors.toMap(Region::getId, Region::getName));
    }
}

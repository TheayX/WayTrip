package com.travel.service.support.recommendation;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.travel.dto.recommendation.config.RecommendationAlgorithmConfigDTO;
import com.travel.dto.recommendation.config.RecommendationCacheConfigDTO;
import com.travel.dto.recommendation.response.SimilarityPreviewResponse;
import com.travel.entity.Order;
import com.travel.entity.Review;
import com.travel.entity.Spot;
import com.travel.entity.UserSpotFavorite;
import com.travel.entity.UserSpotView;
import com.travel.enums.OrderStatus;
import com.travel.mapper.OrderMapper;
import com.travel.mapper.ReviewMapper;
import com.travel.mapper.SpotMapper;
import com.travel.mapper.UserSpotFavoriteMapper;
import com.travel.mapper.UserSpotViewMapper;
import com.travel.service.cache.RecommendationCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 推荐相似度支撑，集中处理相似景点读取、预览组装和离线相似度矩阵更新。
 * <p>
 * 相似度矩阵的读取、构建和预览都收在这里，便于后台调试与定时任务共用同一套逻辑。
 */
@Component
@RequiredArgsConstructor
public class RecommendationSimilaritySupport {

    private final SpotMapper spotMapper;
    private final UserSpotViewMapper userSpotViewMapper;
    private final UserSpotFavoriteMapper userSpotFavoriteMapper;
    private final ReviewMapper reviewMapper;
    private final OrderMapper orderMapper;
    private final RecommendationCacheService recommendationCacheService;
    private final RecommendationQuerySupport recommendationQuerySupport;
    private final RecommendationScoreSupport recommendationScoreSupport;

    @SuppressWarnings("unchecked")
    /**
     * 读取缓存中的相似景点集合。
     *
     * @param spotId 景点 ID
     * @return 相似度映射
     */
    public Map<Long, Double> getSimilarSpots(Long spotId) {
        Object cached = recommendationCacheService.getSimilarity(spotId);
        if (!(cached instanceof Map<?, ?> rawMap) || rawMap.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Long, Double> similarities = new HashMap<>();
        for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
            Long similarSpotId = recommendationScoreSupport.castToLong(entry.getKey());
            Double similarity = recommendationScoreSupport.castToDouble(entry.getValue());
            if (similarSpotId != null && similarity != null) {
                similarities.put(similarSpotId, similarity);
            }
        }
        return similarities;
    }

    /**
     * 查询当前仍可参与推荐的景点 ID。
     *
     * @return 已发布且未删除的景点 ID 集合
     */
    public Set<Long> getActiveSpotIds() {
        return spotMapper.selectList(
            new LambdaQueryWrapper<Spot>()
                .eq(Spot::getIsPublished, 1)
                .eq(Spot::getIsDeleted, 0)
                .select(Spot::getId)
        ).stream().map(Spot::getId).collect(Collectors.toSet());
    }

    /**
     * 基于 IUF 思路计算两景点的相似度。
     *
     * @param usersI 景点 I 的交互用户集合
     * @param usersJ 景点 J 的交互用户集合
     * @param userActivityCount 用户活跃度统计
     * @return 相似度
     */
    public double computeIUFSimilarity(Set<Long> usersI, Set<Long> usersJ, Map<Long, Integer> userActivityCount) {
        Set<Long> smaller = usersI.size() < usersJ.size() ? usersI : usersJ;
        Set<Long> larger = smaller == usersI ? usersJ : usersI;

        double iufSum = 0.0;
        for (Long userId : smaller) {
            if (larger.contains(userId)) {
                int nu = userActivityCount.getOrDefault(userId, 1);
                iufSum += 1.0 / Math.log(1 + nu);
            }
        }

        if (iufSum == 0) {
            return 0;
        }
        double denominator = Math.sqrt(usersI.size()) * Math.sqrt(usersJ.size());
        return iufSum / denominator;
    }

    public SimilarityPreviewResponse buildSimilarityPreview(Long spotId, Integer limit, String lastUpdateTime) {
        int safeLimit = limit == null || limit <= 0 ? 10 : limit;
        Map<Long, Double> similarities = getSimilarSpots(spotId);
        List<Long> neighborIds = similarities.keySet().stream().limit(safeLimit).collect(Collectors.toList());

        SimilarityPreviewResponse response = new SimilarityPreviewResponse();
        response.setSpotId(spotId);
        response.setSpotName(recommendationQuerySupport.getSpotName(spotId));
        response.setTotalNeighbors(similarities.size());
        response.setLastUpdateTime(lastUpdateTime);

        if (neighborIds.isEmpty()) {
            response.setNeighbors(Collections.emptyList());
            return response;
        }

        // 相似邻居预览要补齐展示字段，便于后台直接核对矩阵质量。
        Map<Long, String> categoryMap = recommendationQuerySupport.getCategoryMap();
        Map<Long, String> regionMap = recommendationQuerySupport.getRegionMap();
        Map<Long, Spot> spotMap = spotMapper.selectBatchIds(neighborIds).stream()
            .collect(Collectors.toMap(Spot::getId, spot -> spot));

        response.setNeighbors(neighborIds.stream().map(id -> {
            SimilarityPreviewResponse.NeighborItem item = new SimilarityPreviewResponse.NeighborItem();
            Spot spot = spotMap.get(id);
            item.setSpotId(id);
            item.setSpotName(spot == null ? "未知景点" : spot.getName());
            item.setCoverImage(spot == null ? null : spot.getCoverImageUrl());
            item.setPrice(spot == null ? null : spot.getPrice());
            item.setAvgRating(spot == null ? null : spot.getAvgRating());
            item.setCategoryName(spot == null ? null : categoryMap.get(spot.getCategoryId()));
            item.setRegionName(spot == null ? null : regionMap.get(spot.getRegionId()));
            item.setSimilarity(similarities.get(id));
            return item;
        }).collect(Collectors.toList()));
        return response;
    }

    /**
     * 读取全量行为并构建离线交互矩阵，供后续相似度批量计算复用。
     */
    public OfflineMatrixSnapshot buildOfflineInteractionMatrix(Set<Long> activeSpotIds, RecommendationAlgorithmConfigDTO algorithmConfig) {
        Map<Long, Map<Long, Double>> userItemMatrix = new HashMap<>();
        Map<Long, Map<Long, Double>> viewMatrix = new HashMap<>();
        Map<Long, Map<Long, Double>> favoriteMatrix = new HashMap<>();
        Map<Long, Map<Long, Double>> reviewMatrix = new HashMap<>();
        Map<Long, Map<Long, Double>> orderMatrix = new HashMap<>();
        Set<Long> allSpotIds = new HashSet<>();

        List<UserSpotView> allViews = userSpotViewMapper.selectList(
            new LambdaQueryWrapper<UserSpotView>()
                .select(UserSpotView::getUserId, UserSpotView::getSpotId, UserSpotView::getViewSource, UserSpotView::getViewDuration)
        );
        for (UserSpotView view : allViews) {
            if (!activeSpotIds.contains(view.getSpotId())) {
                continue;
            }
            recommendationScoreSupport.mergeBehaviorWeight(
                viewMatrix.computeIfAbsent(view.getUserId(), key -> new HashMap<>()),
                view.getSpotId(),
                recommendationScoreSupport.calculateViewWeight(view, algorithmConfig)
            );
            allSpotIds.add(view.getSpotId());
        }

        List<UserSpotFavorite> allFavorites = userSpotFavoriteMapper.selectList(
            new LambdaQueryWrapper<UserSpotFavorite>()
                .eq(UserSpotFavorite::getIsDeleted, 0)
                .select(UserSpotFavorite::getUserId, UserSpotFavorite::getSpotId)
        );
        for (UserSpotFavorite favorite : allFavorites) {
            if (!activeSpotIds.contains(favorite.getSpotId())) {
                continue;
            }
            recommendationScoreSupport.mergeBehaviorWeight(
                favoriteMatrix.computeIfAbsent(favorite.getUserId(), key -> new HashMap<>()),
                favorite.getSpotId(),
                algorithmConfig.getWeightFavorite() == null ? 1.0 : algorithmConfig.getWeightFavorite()
            );
            allSpotIds.add(favorite.getSpotId());
        }

        List<Review> allRatings = reviewMapper.selectList(
            new LambdaQueryWrapper<Review>()
                .eq(Review::getIsDeleted, 0)
                .select(Review::getUserId, Review::getSpotId, Review::getScore)
        );
        for (Review review : allRatings) {
            if (!activeSpotIds.contains(review.getSpotId())) {
                continue;
            }
            recommendationScoreSupport.mergeBehaviorWeight(
                reviewMatrix.computeIfAbsent(review.getUserId(), key -> new HashMap<>()),
                review.getSpotId(),
                review.getScore() * (algorithmConfig.getWeightReviewFactor() == null ? 0.4 : algorithmConfig.getWeightReviewFactor())
            );
            allSpotIds.add(review.getSpotId());
        }

        List<Order> allOrders = orderMapper.selectList(
            new LambdaQueryWrapper<Order>()
                .eq(Order::getIsDeleted, 0)
                .in(Order::getStatus, OrderStatus.PAID.getCode(), OrderStatus.COMPLETED.getCode())
                .select(Order::getUserId, Order::getSpotId, Order::getStatus)
        );
        for (Order order : allOrders) {
            if (!activeSpotIds.contains(order.getSpotId())) {
                continue;
            }
            recommendationScoreSupport.mergeBehaviorWeight(
                orderMatrix.computeIfAbsent(order.getUserId(), key -> new HashMap<>()),
                order.getSpotId(),
                order.getStatus() == OrderStatus.COMPLETED.getCode()
                    ? (algorithmConfig.getWeightOrderCompleted() == null ? 4.0 : algorithmConfig.getWeightOrderCompleted())
                    : (algorithmConfig.getWeightOrderPaid() == null ? 3.0 : algorithmConfig.getWeightOrderPaid())
            );
            allSpotIds.add(order.getSpotId());
        }

        mergeBehaviorMatrix(userItemMatrix, viewMatrix);
        mergeBehaviorMatrix(userItemMatrix, favoriteMatrix);
        mergeBehaviorMatrix(userItemMatrix, reviewMatrix);
        mergeBehaviorMatrix(userItemMatrix, orderMatrix);

        recommendationScoreSupport.logUserItemMatrixSamples(userItemMatrix);
        return new OfflineMatrixSnapshot(userItemMatrix, allSpotIds);
    }

    private void mergeBehaviorMatrix(Map<Long, Map<Long, Double>> userItemMatrix, Map<Long, Map<Long, Double>> behaviorMatrix) {
        // 不同行为先各自算权重，再在统一矩阵里按景点合并。
        behaviorMatrix.forEach((userId, spotWeights) -> {
            Map<Long, Double> mergedWeights = userItemMatrix.computeIfAbsent(userId, key -> new HashMap<>());
            recommendationScoreSupport.mergeInteractionWeight(mergedWeights, spotWeights);
        });
    }

    public Map<Long, Integer> summarizeUserActivityCount(Map<Long, Map<Long, Double>> userItemMatrix) {
        Map<Long, Integer> userActivityCount = new HashMap<>();
        for (Map.Entry<Long, Map<Long, Double>> entry : userItemMatrix.entrySet()) {
            userActivityCount.put(entry.getKey(), entry.getValue().size());
        }
        recommendationScoreSupport.logUserActivitySamples(userActivityCount);
        return userActivityCount;
    }

    public Map<Long, Set<Long>> buildSpotUserIndex(Map<Long, Map<Long, Double>> userItemMatrix) {
        Map<Long, Set<Long>> spotUserSets = new HashMap<>();
        for (Map.Entry<Long, Map<Long, Double>> entry : userItemMatrix.entrySet()) {
            Long userId = entry.getKey();
            for (Long spotId : entry.getValue().keySet()) {
                spotUserSets.computeIfAbsent(spotId, key -> new HashSet<>()).add(userId);
            }
        }
        return spotUserSets;
    }

    public void cacheSimilarityNeighbors(
        Set<Long> allSpotIds,
        Map<Long, Set<Long>> spotUserSets,
        Map<Long, Integer> userActivityCount,
        RecommendationAlgorithmConfigDTO algorithmConfig,
        RecommendationCacheConfigDTO cacheConfig
    ) {
        List<Long> spotIdList = new ArrayList<>(allSpotIds);
        int topK = algorithmConfig.getTopKNeighbors() == null ? 20 : Math.max(algorithmConfig.getTopKNeighbors(), 1);
        int simTTL = cacheConfig.getSimilarityTTLHours() == null ? 24 : cacheConfig.getSimilarityTTLHours();

        // 离线阶段按景点两两计算后只缓存 Top-K，避免矩阵无限膨胀。
        for (int i = 0; i < spotIdList.size(); i++) {
            Long spotI = spotIdList.get(i);
            Set<Long> usersI = spotUserSets.getOrDefault(spotI, Collections.emptySet());
            if (usersI.isEmpty()) {
                continue;
            }

            Map<Long, Double> similarities = new HashMap<>();
            for (int j = 0; j < spotIdList.size(); j++) {
                if (i == j) {
                    continue;
                }

                Long spotJ = spotIdList.get(j);
                Set<Long> usersJ = spotUserSets.getOrDefault(spotJ, Collections.emptySet());
                if (usersJ.isEmpty()) {
                    continue;
                }

                double similarity = computeIUFSimilarity(usersI, usersJ, userActivityCount);
                if (similarity > 0) {
                    similarities.put(spotJ, similarity);
                }
            }

            Map<Long, Double> topSimilarities = similarities.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(topK)
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    Map.Entry::getValue,
                    (left, right) -> left,
                    LinkedHashMap::new
                ));

            recommendationCacheService.saveSimilarity(spotI, Objects.requireNonNull(topSimilarities), simTTL);
            recommendationScoreSupport.logSpotSimilaritySummary(spotI, topSimilarities);
        }
    }

    public void saveOfflineSummary(int totalUsers, int totalSpots) {
        Map<String, Object> statusMap = new HashMap<>();
        statusMap.put("lastUpdateTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        statusMap.put("totalUsers", totalUsers);
        statusMap.put("totalSpots", totalSpots);
        recommendationCacheService.saveStatus(statusMap);
    }

    public record OfflineMatrixSnapshot(
        Map<Long, Map<Long, Double>> userItemMatrix,
        Set<Long> allSpotIds
    ) {
    }
}

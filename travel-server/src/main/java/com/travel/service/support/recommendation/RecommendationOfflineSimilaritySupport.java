package com.travel.service.support.recommendation;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.travel.dto.recommendation.model.RecommendationAlgorithmConfigDTO;
import com.travel.dto.recommendation.model.RecommendationCacheConfigDTO;
import com.travel.entity.Order;
import com.travel.entity.Review;
import com.travel.entity.UserSpotFavorite;
import com.travel.entity.UserSpotView;
import com.travel.enums.OrderStatus;
import com.travel.mapper.OrderMapper;
import com.travel.mapper.ReviewMapper;
import com.travel.mapper.UserSpotFavoriteMapper;
import com.travel.mapper.UserSpotViewMapper;
import com.travel.service.cache.RecommendationCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
 * 推荐离线相似度支撑，集中处理离线矩阵构建、倒排索引计算和相似度缓存落库。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RecommendationOfflineSimilaritySupport {

    private final UserSpotViewMapper userSpotViewMapper;
    private final UserSpotFavoriteMapper userSpotFavoriteMapper;
    private final ReviewMapper reviewMapper;
    private final OrderMapper orderMapper;
    private final RecommendationInteractionSupport recommendationInteractionSupport;
    private final RecommendationSimilaritySupport recommendationSimilaritySupport;
    private final RecommendationDebugSupport recommendationDebugSupport;
    private final RecommendationCacheService recommendationCacheService;

    /**
     * 离线构建用户-景点交互矩阵，并返回参与计算的景点集合。
     */
    public OfflineMatrixSnapshot buildUserItemMatrix(Set<Long> activeSpotIds, RecommendationAlgorithmConfigDTO algorithmConfig) {
        Map<Long, Map<Long, Double>> userItemMatrix = new HashMap<>();
        Set<Long> allSpotIds = new HashSet<>();

        List<UserSpotView> allViews = userSpotViewMapper.selectList(
            new LambdaQueryWrapper<UserSpotView>()
                .select(UserSpotView::getUserId, UserSpotView::getSpotId, UserSpotView::getViewSource, UserSpotView::getViewDuration)
        );
        log.info("离线矩阵更新：已读取浏览行为 {} 条", allViews.size());
        for (UserSpotView view : allViews) {
            if (!activeSpotIds.contains(view.getSpotId())) {
                continue;
            }
            recommendationInteractionSupport.mergeInteractionWeight(
                userItemMatrix.computeIfAbsent(view.getUserId(), key -> new HashMap<>()),
                view.getSpotId(),
                recommendationInteractionSupport.calculateViewWeight(view, algorithmConfig)
            );
            allSpotIds.add(view.getSpotId());
        }

        List<UserSpotFavorite> allFavorites = userSpotFavoriteMapper.selectList(
            new LambdaQueryWrapper<UserSpotFavorite>()
                .eq(UserSpotFavorite::getIsDeleted, 0)
                .select(UserSpotFavorite::getUserId, UserSpotFavorite::getSpotId)
        );
        log.info("离线矩阵更新：已读取收藏行为 {} 条", allFavorites.size());
        for (UserSpotFavorite favorite : allFavorites) {
            if (!activeSpotIds.contains(favorite.getSpotId())) {
                continue;
            }
            recommendationInteractionSupport.mergeInteractionWeight(
                userItemMatrix.computeIfAbsent(favorite.getUserId(), key -> new HashMap<>()),
                favorite.getSpotId(),
                defaultDouble(algorithmConfig.getWeightFavorite(), 1.0)
            );
            allSpotIds.add(favorite.getSpotId());
        }

        List<Review> allRatings = reviewMapper.selectList(
            new LambdaQueryWrapper<Review>()
                .eq(Review::getIsDeleted, 0)
                .select(Review::getUserId, Review::getSpotId, Review::getScore)
        );
        log.info("离线矩阵更新：已读取评分行为 {} 条", allRatings.size());
        for (Review review : allRatings) {
            if (!activeSpotIds.contains(review.getSpotId())) {
                continue;
            }
            recommendationInteractionSupport.mergeInteractionWeight(
                userItemMatrix.computeIfAbsent(review.getUserId(), key -> new HashMap<>()),
                review.getSpotId(),
                review.getScore() * defaultDouble(algorithmConfig.getWeightReviewFactor(), 0.4)
            );
            allSpotIds.add(review.getSpotId());
        }

        List<Order> allOrders = orderMapper.selectList(
            new LambdaQueryWrapper<Order>()
                .eq(Order::getIsDeleted, 0)
                .in(Order::getStatus, OrderStatus.PAID.getCode(), OrderStatus.COMPLETED.getCode())
                .select(Order::getUserId, Order::getSpotId, Order::getStatus)
        );
        log.info("离线矩阵更新：已读取订单行为 {} 条", allOrders.size());
        for (Order order : allOrders) {
            if (!activeSpotIds.contains(order.getSpotId())) {
                continue;
            }
            recommendationInteractionSupport.mergeInteractionWeight(
                userItemMatrix.computeIfAbsent(order.getUserId(), key -> new HashMap<>()),
                order.getSpotId(),
                order.getStatus() == OrderStatus.COMPLETED.getCode()
                    ? defaultDouble(algorithmConfig.getWeightOrderCompleted(), 4.0)
                    : defaultDouble(algorithmConfig.getWeightOrderPaid(), 3.0)
            );
            allSpotIds.add(order.getSpotId());
        }

        recommendationDebugSupport.logUserItemMatrixSamples(userItemMatrix);
        return new OfflineMatrixSnapshot(userItemMatrix, allSpotIds);
    }

    public Map<Long, Integer> buildUserActivityCount(Map<Long, Map<Long, Double>> userItemMatrix) {
        Map<Long, Integer> userActivityCount = new HashMap<>();
        for (Map.Entry<Long, Map<Long, Double>> entry : userItemMatrix.entrySet()) {
            userActivityCount.put(entry.getKey(), entry.getValue().size());
        }
        recommendationDebugSupport.logUserActivitySamples(userActivityCount);
        return userActivityCount;
    }

    public Map<Long, Set<Long>> buildSpotUserSets(Map<Long, Map<Long, Double>> userItemMatrix) {
        Map<Long, Set<Long>> spotUserSets = new HashMap<>();
        for (Map.Entry<Long, Map<Long, Double>> entry : userItemMatrix.entrySet()) {
            Long userId = entry.getKey();
            for (Long spotId : entry.getValue().keySet()) {
                spotUserSets.computeIfAbsent(spotId, key -> new HashSet<>()).add(userId);
            }
        }
        return spotUserSets;
    }

    public void persistSimilarityMatrix(
        Set<Long> allSpotIds,
        Map<Long, Set<Long>> spotUserSets,
        Map<Long, Integer> userActivityCount,
        RecommendationAlgorithmConfigDTO algorithmConfig,
        RecommendationCacheConfigDTO cacheConfig
    ) {
        List<Long> spotIdList = new ArrayList<>(allSpotIds);
        int topK = defaultInt(algorithmConfig.getTopKNeighbors(), 20);
        int simTTL = defaultInt(cacheConfig.getSimilarityTTLHours(), 24);

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

                double similarity = recommendationSimilaritySupport.computeIUFSimilarity(usersI, usersJ, userActivityCount);
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
            recommendationDebugSupport.logSpotSimilaritySummary(spotI, topSimilarities);
        }
    }

    public void saveOfflineStatus(int totalUsers, int totalSpots) {
        Map<String, Object> statusMap = new HashMap<>();
        statusMap.put("lastUpdateTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        statusMap.put("totalUsers", totalUsers);
        statusMap.put("totalSpots", totalSpots);
        recommendationCacheService.saveStatus(statusMap);
    }

    private double defaultDouble(Double value, double fallback) {
        return value == null ? fallback : value;
    }

    private int defaultInt(Integer value, int fallback) {
        return value == null ? fallback : value;
    }

    public record OfflineMatrixSnapshot(
        Map<Long, Map<Long, Double>> userItemMatrix,
        Set<Long> allSpotIds
    ) {
    }
}

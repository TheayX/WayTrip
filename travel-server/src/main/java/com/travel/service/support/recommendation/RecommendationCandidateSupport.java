package com.travel.service.support.recommendation;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.travel.dto.recommendation.model.RecommendationHeatConfigDTO;
import com.travel.entity.Order;
import com.travel.entity.Review;
import com.travel.entity.Spot;
import com.travel.entity.UserSpotFavorite;
import com.travel.enums.OrderStatus;
import com.travel.mapper.OrderMapper;
import com.travel.mapper.ReviewMapper;
import com.travel.mapper.SpotMapper;
import com.travel.mapper.UserSpotFavoriteMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 推荐候选支撑，集中处理候选过滤、热度重排和缓存分数转换。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RecommendationCandidateSupport {

    private final SpotMapper spotMapper;
    private final ReviewMapper reviewMapper;
    private final UserSpotFavoriteMapper userSpotFavoriteMapper;
    private final OrderMapper orderMapper;
    private final RecommendationDebugSupport recommendationDebugSupport;

    public Map<Long, Double> applyHeatRerank(Map<Long, Double> scoreMap, RecommendationHeatConfigDTO config, boolean debug) {
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
            recommendationDebugSupport.logHeatRerankDetails(scoreMap, rerankedScores, heatMap, rerankFactor, maxHeat);
        }
        return rerankedScores;
    }

    /**
     * 过滤用户已经发生过强交互的景点，避免把已明确消费过的候选继续推回用户。
     */
    public List<Long> filterInteractedSpots(Long userId, List<Long> spotIds) {
        if (spotIds == null || spotIds.isEmpty()) {
            return spotIds;
        }

        Set<Long> ratedIds = reviewMapper.selectList(
            new LambdaQueryWrapper<Review>()
                .eq(Review::getUserId, userId)
                .eq(Review::getIsDeleted, 0)
        ).stream().map(Review::getSpotId).collect(Collectors.toSet());

        Set<Long> favoriteIds = userSpotFavoriteMapper.selectList(
            new LambdaQueryWrapper<UserSpotFavorite>()
                .eq(UserSpotFavorite::getUserId, userId)
                .eq(UserSpotFavorite::getIsDeleted, 0)
        ).stream().map(UserSpotFavorite::getSpotId).collect(Collectors.toSet());

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

    public Map<Long, Double> orderScoresByIds(List<Long> orderedIds, Map<Long, Double> scoreMap) {
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

    public Map<Long, Double> castScoreMap(Map<?, ?> rawMap) {
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

    public Long castToLong(Object value) {
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

    public Double castToDouble(Object value) {
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
}

package com.travel.service.support.recommendation;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.travel.dto.recommendation.response.SimilarityPreviewResponse;
import com.travel.entity.Spot;
import com.travel.mapper.SpotMapper;
import com.travel.service.cache.RecommendationCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 推荐相似度支撑，集中处理相似景点读取、预览组装和相似度基础计算。
 */
@Component
@RequiredArgsConstructor
public class RecommendationSimilaritySupport {

    private final SpotMapper spotMapper;
    private final RecommendationCacheService recommendationCacheService;
    private final RecommendationMetadataSupport recommendationMetadataSupport;

    @SuppressWarnings("unchecked")
    public Map<Long, Double> getSimilarSpots(Long spotId) {
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

    public Set<Long> getActiveSpotIds() {
        return spotMapper.selectList(
            new LambdaQueryWrapper<Spot>()
                .eq(Spot::getIsPublished, 1)
                .eq(Spot::getIsDeleted, 0)
                .select(Spot::getId)
        ).stream().map(Spot::getId).collect(Collectors.toSet());
    }

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
        response.setSpotName(recommendationMetadataSupport.getSpotName(spotId));
        response.setTotalNeighbors(similarities.size());
        response.setLastUpdateTime(lastUpdateTime);

        if (neighborIds.isEmpty()) {
            response.setNeighbors(Collections.emptyList());
            return response;
        }

        Map<Long, String> categoryMap = recommendationMetadataSupport.getCategoryMap();
        Map<Long, String> regionMap = recommendationMetadataSupport.getRegionMap();
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
        if (value instanceof String stringValue) {
            try {
                return Long.parseLong(stringValue);
            } catch (NumberFormatException ignored) {
                return null;
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
        if (value instanceof String stringValue) {
            try {
                return Double.parseDouble(stringValue);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }
}

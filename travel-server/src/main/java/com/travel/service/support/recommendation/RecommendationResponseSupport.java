package com.travel.service.support.recommendation;

import com.travel.dto.recommendation.response.RecommendationResponse;
import com.travel.entity.Spot;
import com.travel.mapper.SpotMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 推荐结果响应支撑，集中处理推荐结果列表的元数据补充和响应组装。
 */
@Component
@RequiredArgsConstructor
public class RecommendationResponseSupport {

    private final SpotMapper spotMapper;
    private final RecommendationMetadataSupport recommendationMetadataSupport;

    public RecommendationResponse buildRecommendationResponse(
        List<Long> spotIds,
        Map<Long, Double> scoreMap,
        Integer limit,
        String type,
        Boolean needPreference,
        RecommendationResponse.DebugInfo debugInfo
    ) {
        List<Long> limitedIds = spotIds.stream().limit(limit).collect(Collectors.toList());

        RecommendationResponse response = new RecommendationResponse();
        response.setType(type);
        response.setNeedPreference(needPreference);
        response.setDebugInfo(debugInfo);

        if (limitedIds.isEmpty()) {
            response.setList(Collections.emptyList());
            return response;
        }

        List<Spot> spots = spotMapper.selectBatchIds(limitedIds);
        Map<Long, String> categoryMap = recommendationMetadataSupport.getCategoryMap();
        Map<Long, String> regionMap = recommendationMetadataSupport.getRegionMap();
        Map<Long, Spot> spotMap = spots.stream().collect(Collectors.toMap(Spot::getId, spot -> spot));

        if (debugInfo != null) {
            debugInfo.setFinalCount(limitedIds.size());
        }

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
}

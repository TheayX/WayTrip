package com.travel.service.support.recommendation;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.travel.dto.recommendation.response.RecommendationResponse;
import com.travel.entity.Spot;
import com.travel.entity.SpotCategory;
import com.travel.entity.SpotRegion;
import com.travel.mapper.SpotCategoryMapper;
import com.travel.mapper.SpotMapper;
import com.travel.mapper.SpotRegionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 推荐查询支撑，集中处理景点名称、分类地区字典和推荐响应组装。
 */
@Component
@RequiredArgsConstructor
public class RecommendationQuerySupport {

    private final SpotMapper spotMapper;
    private final SpotCategoryMapper categoryMapper;
    private final SpotRegionMapper spotRegionMapper;

    public Map<Long, String> getCategoryMap() {
        return categoryMapper.selectList(new LambdaQueryWrapper<SpotCategory>().eq(SpotCategory::getIsDeleted, 0)).stream()
            .collect(Collectors.toMap(SpotCategory::getId, SpotCategory::getName));
    }

    public Map<Long, String> getRegionMap() {
        return spotRegionMapper.selectList(new LambdaQueryWrapper<SpotRegion>().eq(SpotRegion::getIsDeleted, 0)).stream()
            .collect(Collectors.toMap(SpotRegion::getId, SpotRegion::getName));
    }

    public String getSpotName(Long spotId) {
        if (spotId == null) {
            return "未知景点";
        }
        Spot spot = spotMapper.selectById(spotId);
        return spot == null || spot.getName() == null ? "未知景点" : spot.getName();
    }

    /**
     * 按推荐结果顺序补齐景点元数据，并组装统一的推荐响应对象。
     */
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
        Map<Long, String> categoryMap = getCategoryMap();
        Map<Long, String> regionMap = getRegionMap();
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

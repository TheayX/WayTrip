package com.travel.service.support.spot;

import com.travel.dto.spot.response.AdminSpotListResponse;
import com.travel.dto.spot.response.SpotListResponse;
import com.travel.entity.Spot;
import com.travel.entity.SpotCategory;
import com.travel.entity.SpotRegion;
import com.travel.mapper.SpotCategoryMapper;
import com.travel.mapper.SpotRegionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 景点响应装配器，统一处理景点列表响应和分类地区名称补齐。
 */
@Component
@RequiredArgsConstructor
public class SpotResponseAssembler {

    private final SpotRegionMapper spotRegionMapper;
    private final SpotCategoryMapper spotCategoryMapper;

    /**
     * 用户端列表项保持轻量字段，避免装配逻辑散落在查询服务里。
     */
    public SpotListResponse toSpotListResponse(Spot spot) {
        return SpotListResponse.builder()
            .id(spot.getId())
            .name(spot.getName())
            .coverImage(spot.getCoverImageUrl())
            .price(spot.getPrice())
            .avgRating(spot.getAvgRating())
            .ratingCount(spot.getRatingCount())
            .regionName(getRegionName(spot.getRegionId()))
            .categoryName(getCategoryName(spot.getCategoryId()))
            .build();
    }

    /**
     * 管理端列表项补齐后台关注字段，和用户端响应装配分开处理。
     */
    public AdminSpotListResponse toAdminSpotListResponse(Spot spot) {
        return AdminSpotListResponse.builder()
            .id(spot.getId())
            .name(spot.getName())
            .coverImage(spot.getCoverImageUrl())
            .price(spot.getPrice())
            .regionName(getRegionName(spot.getRegionId()))
            .categoryName(getCategoryName(spot.getCategoryId()))
            .avgRating(spot.getAvgRating())
            .ratingCount(spot.getRatingCount())
            .heatLevel(spot.getHeatLevel())
            .heatScore(spot.getHeatScore())
            .published(spot.getIsPublished() == 1)
            .createdAt(spot.getCreatedAt())
            .updatedAt(spot.getUpdatedAt())
            .build();
    }

    /**
     * 名称补齐统一走这里，避免查询层重复直接访问字典表。
     */
    public String getRegionName(Long regionId) {
        if (regionId == null) {
            return null;
        }
        SpotRegion region = spotRegionMapper.selectById(regionId);
        return region != null && region.getIsDeleted() == 0 ? region.getName() : null;
    }

    /**
     * 分类名称读取与地区保持同一入口，减少装配代码分叉。
     */
    public String getCategoryName(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        SpotCategory category = spotCategoryMapper.selectById(categoryId);
        return category != null && category.getIsDeleted() == 0 ? category.getName() : null;
    }
}

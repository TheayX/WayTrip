package com.travel.service.support.spot;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.travel.dto.spot.request.AdminSpotUpsertRequest;
import com.travel.dto.spot.response.AdminSpotListResponse;
import com.travel.dto.spot.response.SpotFilterResponse;
import com.travel.dto.spot.response.SpotListResponse;
import com.travel.entity.Spot;
import com.travel.entity.SpotCategory;
import com.travel.entity.SpotImage;
import com.travel.entity.SpotRegion;
import com.travel.mapper.SpotCategoryMapper;
import com.travel.mapper.SpotImageMapper;
import com.travel.mapper.SpotRegionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 景点服务通用支撑，集中放查询和管理共用的转换、树组装和层级查找逻辑。
 */
@Component
@RequiredArgsConstructor
public class SpotSupportService {

    private final SpotImageMapper spotImageMapper;
    private final SpotRegionMapper spotRegionMapper;
    private final SpotCategoryMapper spotCategoryMapper;

    public SpotListResponse convertToListResponse(Spot spot) {
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

    public AdminSpotListResponse convertToAdminListResponse(Spot spot) {
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

    public String getRegionName(Long regionId) {
        if (regionId == null) {
            return null;
        }
        SpotRegion region = spotRegionMapper.selectById(regionId);
        return region != null && region.getIsDeleted() == 0 ? region.getName() : null;
    }

    public String getCategoryName(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        SpotCategory category = spotCategoryMapper.selectById(categoryId);
        return category != null && category.getIsDeleted() == 0 ? category.getName() : null;
    }

    public Set<Long> findCategoryAndChildrenIds(Long categoryId) {
        List<SpotCategory> categories = spotCategoryMapper.selectList(
            new LambdaQueryWrapper<SpotCategory>()
                .eq(SpotCategory::getIsDeleted, 0)
                .select(SpotCategory::getId, SpotCategory::getParentId)
        );

        Map<Long, List<Long>> childrenMap = new HashMap<>();
        for (SpotCategory category : categories) {
            Long parentId = category.getParentId();
            if (parentId != null && parentId > 0) {
                childrenMap.computeIfAbsent(parentId, key -> new ArrayList<>()).add(category.getId());
            }
        }

        return collectTreeIds(categoryId, childrenMap);
    }

    public Set<Long> findRegionAndChildrenIds(Long regionId) {
        List<SpotRegion> regions = spotRegionMapper.selectList(
            new LambdaQueryWrapper<SpotRegion>()
                .eq(SpotRegion::getIsDeleted, 0)
                .select(SpotRegion::getId, SpotRegion::getParentId)
        );

        Map<Long, List<Long>> childrenMap = new HashMap<>();
        for (SpotRegion region : regions) {
            Long parentId = region.getParentId();
            if (parentId != null && parentId > 0) {
                childrenMap.computeIfAbsent(parentId, key -> new ArrayList<>()).add(region.getId());
            }
        }

        return collectTreeIds(regionId, childrenMap);
    }

    public SpotFilterResponse getFilters() {
        List<SpotRegion> regions = spotRegionMapper.selectList(
            new LambdaQueryWrapper<SpotRegion>()
                .eq(SpotRegion::getIsDeleted, 0)
                .orderByAsc(SpotRegion::getSortOrder)
        );
        List<SpotCategory> categories = spotCategoryMapper.selectList(
            new LambdaQueryWrapper<SpotCategory>()
                .eq(SpotCategory::getIsDeleted, 0)
                .orderByAsc(SpotCategory::getSortOrder)
                .orderByAsc(SpotCategory::getId)
        );

        List<SpotFilterResponse.FilterItem> regionItems = regions.stream()
            .map(region -> SpotFilterResponse.FilterItem.builder()
                .id(region.getId())
                .name(region.getName())
                .parentId(region.getParentId())
                .children(new ArrayList<>())
                .build())
            .collect(Collectors.toList());

        List<SpotFilterResponse.FilterItem> categoryItems = categories.stream()
            .map(this::convertCategoryFilterItem)
            .collect(Collectors.toList());

        List<SpotFilterResponse.FilterItem> regionTree = buildFilterTree(regionItems);
        Set<Long> regionParentIds = regionItems.stream()
            .map(SpotFilterResponse.FilterItem::getParentId)
            .filter(parentId -> parentId != null && parentId > 0)
            .collect(Collectors.toSet());
        List<SpotFilterResponse.FilterItem> regionLeaves = regionItems.stream()
            .filter(item -> !regionParentIds.contains(item.getId()))
            .collect(Collectors.toList());

        return SpotFilterResponse.builder()
            .regions(regionLeaves)
            .regionTree(regionTree)
            .categories(categoryItems)
            .categoryTree(buildFilterTree(categoryItems))
            .build();
    }

    public void copyProperties(AdminSpotUpsertRequest request, Spot spot) {
        if (request.getName() != null) {
            spot.setName(request.getName());
        }
        if (request.getDescription() != null) {
            spot.setDescription(request.getDescription());
        }
        if (request.getPrice() != null) {
            spot.setPrice(request.getPrice());
        }
        if (request.getOpenTime() != null) {
            spot.setOpenTime(request.getOpenTime());
        }
        if (request.getAddress() != null) {
            spot.setAddress(request.getAddress());
        }
        if (request.getLatitude() != null) {
            spot.setLatitude(request.getLatitude());
        }
        if (request.getLongitude() != null) {
            spot.setLongitude(request.getLongitude());
        }
        if (request.getCoverImage() != null) {
            spot.setCoverImageUrl(request.getCoverImage());
        }
        if (request.getRegionId() != null) {
            spot.setRegionId(request.getRegionId());
        }
        if (request.getCategoryId() != null) {
            spot.setCategoryId(request.getCategoryId());
        }
        if (request.getPublished() != null) {
            spot.setIsPublished(Boolean.TRUE.equals(request.getPublished()) ? 1 : 0);
        }
        if (request.getHeatLevel() != null) {
            spot.setHeatLevel(request.getHeatLevel());
        }
    }

    public void saveSpotImages(Long spotId, List<String> images) {
        if (images == null || images.isEmpty()) {
            return;
        }
        for (int i = 0; i < images.size(); i++) {
            SpotImage image = new SpotImage();
            image.setSpotId(spotId);
            image.setImageUrl(images.get(i));
            image.setSortOrder(i + 1);
            spotImageMapper.insert(image);
        }
    }

    private SpotFilterResponse.FilterItem convertCategoryFilterItem(SpotCategory category) {
        return SpotFilterResponse.FilterItem.builder()
            .id(category.getId())
            .name(category.getName())
            .parentId(category.getParentId())
            .iconUrl(category.getIconUrl())
            .children(new ArrayList<>())
            .build();
    }

    private List<SpotFilterResponse.FilterItem> buildFilterTree(List<SpotFilterResponse.FilterItem> items) {
        Map<Long, SpotFilterResponse.FilterItem> itemMap = items.stream()
            .collect(Collectors.toMap(SpotFilterResponse.FilterItem::getId, item -> item));

        List<SpotFilterResponse.FilterItem> roots = new ArrayList<>();
        for (SpotFilterResponse.FilterItem item : items) {
            Long parentId = item.getParentId();
            if (parentId == null || parentId <= 0 || !itemMap.containsKey(parentId)) {
                roots.add(item);
                continue;
            }
            SpotFilterResponse.FilterItem parent = itemMap.get(parentId);
            if (parent.getChildren() == null) {
                parent.setChildren(new ArrayList<>());
            }
            parent.getChildren().add(item);
        }
        return roots;
    }

    private Set<Long> collectTreeIds(Long rootId, Map<Long, List<Long>> childrenMap) {
        Set<Long> allIds = new HashSet<>();
        List<Long> stack = new ArrayList<>();
        stack.add(rootId);
        while (!stack.isEmpty()) {
            Long currentId = stack.remove(stack.size() - 1);
            if (!allIds.add(currentId)) {
                continue;
            }
            List<Long> children = childrenMap.get(currentId);
            if (children != null && !children.isEmpty()) {
                stack.addAll(children);
            }
        }
        return allIds;
    }
}

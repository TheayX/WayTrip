package com.travel.service.support.spot;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.travel.dto.spot.response.SpotFilterResponse;
import com.travel.entity.SpotCategory;
import com.travel.entity.SpotRegion;
import com.travel.mapper.SpotCategoryMapper;
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
 * 景点树形支撑，集中处理分类地区树组装和层级 ID 查找。
 */
@Component
@RequiredArgsConstructor
public class SpotTreeSupport {

    private final SpotRegionMapper spotRegionMapper;
    private final SpotCategoryMapper spotCategoryMapper;

    /**
     * 分类筛选既要支持父分类，也要支持子分类一并命中。
     */
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

    /**
     * 地区筛选与分类筛选保持同样的层级展开规则。
     */
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

    /**
     * 统一组装筛选树，保证用户端和后台读取到的层级结构口径一致。
     */
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

package com.travel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.travel.common.exception.BusinessException;
import com.travel.dto.region.request.AdminRegionRequest;
import com.travel.dto.region.response.AdminRegionResponse;
import com.travel.entity.SpotRegion;
import com.travel.mapper.SpotRegionMapper;
import com.travel.service.SpotRegionService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 景点地区服务实现，负责管理端地区查询与层级维护。
 */
@Service
public class SpotRegionServiceImpl extends ServiceImpl<SpotRegionMapper, SpotRegion> implements SpotRegionService {

    // 管理端地区查询与维护

    @Override
    public List<AdminRegionResponse> getAdminRegions(Long parentId) {
        LambdaQueryWrapper<SpotRegion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SpotRegion::getIsDeleted, 0);
        if (parentId != null) {
            wrapper.eq(SpotRegion::getParentId, parentId);
        }
        wrapper.orderByAsc(SpotRegion::getSortOrder)
               .orderByDesc(SpotRegion::getCreatedAt);
               
        List<SpotRegion> regions = list(wrapper);
        return regions.stream().map(region -> {
            AdminRegionResponse response = new AdminRegionResponse();
            BeanUtils.copyProperties(Objects.requireNonNull(region), response);
            return response;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createRegion(AdminRegionRequest request) {
        AdminRegionRequest validRequest = Objects.requireNonNull(request);
        int targetSortOrder = prepareInsertSortOrder(validRequest.getParentId(), validRequest.getSortOrder());
        SpotRegion region = new SpotRegion();
        BeanUtils.copyProperties(validRequest, region);
        region.setSortOrder(targetSortOrder);
        region.setIsDeleted(0);
        save(region);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateRegion(Long id, AdminRegionRequest request) {
        AdminRegionRequest validRequest = Objects.requireNonNull(request);
        SpotRegion region = getActiveRegion(id);
        int targetSortOrder = prepareUpdateSortOrder(region, validRequest);
        BeanUtils.copyProperties(validRequest, region);
        region.setId(id);
        region.setSortOrder(targetSortOrder);
        updateById(region);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRegion(Long id) {
        SpotRegion region = getById(id);
        if (region == null || region.getIsDeleted() == 1) {
            return;
        }
        
        // 删除前需要先保证当前地区下不存在子地区，避免树结构残缺。
        LambdaQueryWrapper<SpotRegion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SpotRegion::getParentId, id)
               .eq(SpotRegion::getIsDeleted, 0);
        if (count(wrapper) > 0) {
            throw new BusinessException(400, "该地区下还存在子地区，请先删除子地区");
        }
        
        region.setIsDeleted(1);
        updateById(region);
    }

    /**
     * 地区编辑必须命中有效节点，统一收口节点存在性判断。
     */
    private SpotRegion getActiveRegion(Long id) {
        SpotRegion region = getById(id);
        if (region == null || region.getIsDeleted() == 1) {
            throw new BusinessException(404, "地区不存在");
        }
        return region;
    }

    /**
     * 新增地区时自动规范目标排序，并对同级后续项执行顺延。
     */
    private int prepareInsertSortOrder(Long parentId, Integer requestedSortOrder) {
        List<SpotRegion> siblings = listActiveRegionsByParent(parentId);
        int maxSortOrder = siblings.stream()
            .map(SpotRegion::getSortOrder)
            .filter(Objects::nonNull)
            .max(Integer::compareTo)
            .orElse(0);
        int targetSortOrder = normalizeInsertSortOrder(requestedSortOrder, maxSortOrder);
        if (targetSortOrder <= maxSortOrder) {
            // 目标位已被占用时，后续同级节点整体后移，避免出现重复序号。
            siblings.stream()
                .filter(item -> safeSortOrder(item.getSortOrder()) >= targetSortOrder)
                .sorted((left, right) -> Integer.compare(safeSortOrder(right.getSortOrder()), safeSortOrder(left.getSortOrder())))
                .forEach(item -> {
                    item.setSortOrder(safeSortOrder(item.getSortOrder()) + 1);
                    updateById(item);
                });
        }
        return targetSortOrder;
    }

    /**
     * 编辑地区时，根据是否跨父节点调整原位置和目标位置的同级排序。
     */
    private int prepareUpdateSortOrder(SpotRegion currentRegion, AdminRegionRequest request) {
        Long originalParentId = currentRegion.getParentId();
        Long targetParentId = request.getParentId();
        int originalSortOrder = safeSortOrder(currentRegion.getSortOrder());

        if (Objects.equals(originalParentId, targetParentId)) {
            return reorderWithinSameParent(currentRegion.getId(), targetParentId, originalSortOrder, request.getSortOrder());
        }

        compactSortOrdersAfterRemoval(originalParentId, currentRegion.getId(), originalSortOrder);
        return prepareInsertSortOrder(targetParentId, request.getSortOrder());
    }

    /**
     * 同父节点内调整排序时，只移动受影响区间，避免无关数据被改写。
     */
    private int reorderWithinSameParent(Long currentId, Long parentId, int originalSortOrder, Integer requestedSortOrder) {
        List<SpotRegion> siblings = listActiveRegionsByParent(parentId).stream()
            .filter(item -> !Objects.equals(item.getId(), currentId))
            .collect(Collectors.toList());
        int maxSortOrder = siblings.stream()
            .map(SpotRegion::getSortOrder)
            .filter(Objects::nonNull)
            .max(Integer::compareTo)
            .orElse(0);
        int targetSortOrder = normalizeUpdateSortOrder(requestedSortOrder, maxSortOrder + 1);

        if (targetSortOrder < originalSortOrder) {
            siblings.stream()
                .filter(item -> {
                    int sortOrder = safeSortOrder(item.getSortOrder());
                    return sortOrder >= targetSortOrder && sortOrder < originalSortOrder;
                })
                .sorted((left, right) -> Integer.compare(safeSortOrder(right.getSortOrder()), safeSortOrder(left.getSortOrder())))
                .forEach(item -> {
                    item.setSortOrder(safeSortOrder(item.getSortOrder()) + 1);
                    updateById(item);
                });
        } else if (targetSortOrder > originalSortOrder) {
            siblings.stream()
                .filter(item -> {
                    int sortOrder = safeSortOrder(item.getSortOrder());
                    return sortOrder > originalSortOrder && sortOrder <= targetSortOrder;
                })
                .sorted((left, right) -> Integer.compare(safeSortOrder(left.getSortOrder()), safeSortOrder(right.getSortOrder())))
                .forEach(item -> {
                    item.setSortOrder(safeSortOrder(item.getSortOrder()) - 1);
                    updateById(item);
                });
        }

        return targetSortOrder;
    }

    /**
     * 节点迁移或删除后补齐原父节点排序空位，保证同级序号连续。
     */
    private void compactSortOrdersAfterRemoval(Long parentId, Long currentId, int removedSortOrder) {
        listActiveRegionsByParent(parentId).stream()
            .filter(item -> !Objects.equals(item.getId(), currentId))
            .filter(item -> safeSortOrder(item.getSortOrder()) > removedSortOrder)
            .sorted((left, right) -> Integer.compare(safeSortOrder(left.getSortOrder()), safeSortOrder(right.getSortOrder())))
            .forEach(item -> {
                item.setSortOrder(safeSortOrder(item.getSortOrder()) - 1);
                updateById(item);
            });
    }

    /**
     * 查询指定父节点下的有效地区，作为同级重排的基础数据。
     */
    private List<SpotRegion> listActiveRegionsByParent(Long parentId) {
        LambdaQueryWrapper<SpotRegion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SpotRegion::getIsDeleted, 0)
            .eq(SpotRegion::getParentId, parentId)
            .orderByAsc(SpotRegion::getSortOrder)
            .orderByAsc(SpotRegion::getId);
        return list(wrapper);
    }

    /**
     * 新增时允许插入到中间位置，超出范围则自动落到末尾。
     */
    private int normalizeInsertSortOrder(Integer requestedSortOrder, int maxSortOrder) {
        if (requestedSortOrder == null) {
            return maxSortOrder + 1;
        }
        int normalized = Math.max(1, requestedSortOrder);
        return Math.min(normalized, maxSortOrder + 1);
    }

    /**
     * 编辑时保留插队能力，同时限制在当前同级可用范围内。
     */
    private int normalizeUpdateSortOrder(Integer requestedSortOrder, int maxSortOrder) {
        if (requestedSortOrder == null) {
            return maxSortOrder;
        }
        int normalized = Math.max(1, requestedSortOrder);
        return Math.min(normalized, Math.max(1, maxSortOrder));
    }

    /**
     * 兼容历史空排序数据，避免重排时出现空指针。
     */
    private int safeSortOrder(Integer sortOrder) {
        return sortOrder == null ? 1 : sortOrder;
    }
}

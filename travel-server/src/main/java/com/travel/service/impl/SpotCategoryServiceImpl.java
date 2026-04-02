package com.travel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.travel.common.exception.BusinessException;
import com.travel.dto.category.request.AdminCategoryRequest;
import com.travel.dto.category.response.AdminCategoryResponse;
import com.travel.entity.SpotCategory;
import com.travel.mapper.SpotCategoryMapper;
import com.travel.service.SpotCategoryService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 景点分类服务实现，负责管理端分类查询与层级维护。
 */
@Service
public class SpotCategoryServiceImpl extends ServiceImpl<SpotCategoryMapper, SpotCategory> implements SpotCategoryService {

    // 管理端分类查询与维护

    @Override
    public List<AdminCategoryResponse> getAdminCategories(Long parentId) {
        LambdaQueryWrapper<SpotCategory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SpotCategory::getIsDeleted, 0);
        if (parentId != null) {
            wrapper.eq(SpotCategory::getParentId, parentId);
        }
        wrapper.orderByAsc(SpotCategory::getSortOrder)
               .orderByDesc(SpotCategory::getCreatedAt);
               
        List<SpotCategory> categories = list(wrapper);
        return categories.stream().map(category -> {
            AdminCategoryResponse response = new AdminCategoryResponse();
            BeanUtils.copyProperties(Objects.requireNonNull(category), response);
            return response;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createCategory(AdminCategoryRequest request) {
        AdminCategoryRequest validRequest = Objects.requireNonNull(request);
        int targetSortOrder = prepareInsertSortOrder(validRequest.getParentId(), validRequest.getSortOrder());
        SpotCategory category = new SpotCategory();
        BeanUtils.copyProperties(validRequest, category);
        category.setSortOrder(targetSortOrder);
        category.setIsDeleted(0);
        save(category);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCategory(Long id, AdminCategoryRequest request) {
        AdminCategoryRequest validRequest = Objects.requireNonNull(request);
        SpotCategory category = getActiveCategory(id);
        int targetSortOrder = prepareUpdateSortOrder(category, validRequest);
        BeanUtils.copyProperties(validRequest, category);
        category.setId(id);
        category.setSortOrder(targetSortOrder);
        updateById(category);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCategory(Long id) {
        SpotCategory category = getById(id);
        if (category == null || category.getIsDeleted() == 1) {
            return;
        }
        
        // 删除前需要先保证当前分类下不存在子分类，避免树结构残缺。
        LambdaQueryWrapper<SpotCategory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SpotCategory::getParentId, id)
               .eq(SpotCategory::getIsDeleted, 0);
        if (count(wrapper) > 0) {
            throw new BusinessException(400, "该分类下还存在子分类，请先删除子分类");
        }
        
        category.setIsDeleted(1);
        updateById(category);
    }

    /**
     * 分类编辑必须命中有效节点，统一收口节点存在性判断。
     */
    private SpotCategory getActiveCategory(Long id) {
        SpotCategory category = getById(id);
        if (category == null || category.getIsDeleted() == 1) {
            throw new BusinessException(404, "分类不存在");
        }
        return category;
    }

    /**
     * 新增分类时自动规范目标排序，并对同级后续项执行顺延。
     */
    private int prepareInsertSortOrder(Long parentId, Integer requestedSortOrder) {
        List<SpotCategory> siblings = listActiveCategoriesByParent(parentId);
        int maxSortOrder = siblings.stream()
            .map(SpotCategory::getSortOrder)
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
     * 编辑分类时，根据是否跨父节点调整原位置和目标位置的同级排序。
     */
    private int prepareUpdateSortOrder(SpotCategory currentCategory, AdminCategoryRequest request) {
        Long originalParentId = currentCategory.getParentId();
        Long targetParentId = request.getParentId();
        int originalSortOrder = safeSortOrder(currentCategory.getSortOrder());

        if (Objects.equals(originalParentId, targetParentId)) {
            return reorderWithinSameParent(currentCategory.getId(), targetParentId, originalSortOrder, request.getSortOrder());
        }

        compactSortOrdersAfterRemoval(originalParentId, currentCategory.getId(), originalSortOrder);
        return prepareInsertSortOrder(targetParentId, request.getSortOrder());
    }

    /**
     * 同父节点内调整排序时，只移动受影响区间，避免无关数据被改写。
     */
    private int reorderWithinSameParent(Long currentId, Long parentId, int originalSortOrder, Integer requestedSortOrder) {
        List<SpotCategory> siblings = listActiveCategoriesByParent(parentId).stream()
            .filter(item -> !Objects.equals(item.getId(), currentId))
            .collect(Collectors.toList());
        int maxSortOrder = siblings.stream()
            .map(SpotCategory::getSortOrder)
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
        listActiveCategoriesByParent(parentId).stream()
            .filter(item -> !Objects.equals(item.getId(), currentId))
            .filter(item -> safeSortOrder(item.getSortOrder()) > removedSortOrder)
            .sorted((left, right) -> Integer.compare(safeSortOrder(left.getSortOrder()), safeSortOrder(right.getSortOrder())))
            .forEach(item -> {
                item.setSortOrder(safeSortOrder(item.getSortOrder()) - 1);
                updateById(item);
            });
    }

    /**
     * 查询指定父节点下的有效分类，作为同级重排的基础数据。
     */
    private List<SpotCategory> listActiveCategoriesByParent(Long parentId) {
        LambdaQueryWrapper<SpotCategory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SpotCategory::getIsDeleted, 0)
            .eq(SpotCategory::getParentId, parentId)
            .orderByAsc(SpotCategory::getSortOrder)
            .orderByAsc(SpotCategory::getId);
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

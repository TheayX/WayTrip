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
        SpotCategory category = new SpotCategory();
        BeanUtils.copyProperties(Objects.requireNonNull(request), category);
        category.setIsDeleted(0);
        save(category);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCategory(Long id, AdminCategoryRequest request) {
        SpotCategory category = getById(id);
        if (category == null || category.getIsDeleted() == 1) {
            throw new BusinessException(404, "分类不存在");
        }
        BeanUtils.copyProperties(Objects.requireNonNull(request), category);
        category.setId(id);
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
}

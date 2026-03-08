package com.travel.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.travel.dto.category.AdminCategoryRequest;
import com.travel.dto.category.AdminCategoryResponse;
import com.travel.entity.SpotCategory;

import java.util.List;

public interface SpotCategoryService extends IService<SpotCategory> {
    
    /**
     * 根据父级ID获取分类列表
     */
    List<AdminCategoryResponse> getAdminCategories(Long parentId);
    
    /**
     * 创建分类
     */
    void createCategory(AdminCategoryRequest request);
    
    /**
     * 更新分类
     */
    void updateCategory(Long id, AdminCategoryRequest request);
    
    /**
     * 删除分类
     */
    void deleteCategory(Long id);
}

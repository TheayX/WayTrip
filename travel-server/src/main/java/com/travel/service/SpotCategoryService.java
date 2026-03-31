package com.travel.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.travel.dto.category.request.AdminCategoryRequest;
import com.travel.dto.category.response.AdminCategoryResponse;
import com.travel.entity.SpotCategory;

import java.util.List;

/**
 * 景点分类服务接口。
 * <p>
 * 定义管理端分类树查询、创建、更新和删除能力。
 */
public interface SpotCategoryService extends IService<SpotCategory> {
    
    /**
     * 根据父级 ID 获取分类列表。
     *
     * @param parentId 父级分类 ID
     * @return 分类列表
     */
    List<AdminCategoryResponse> getAdminCategories(Long parentId);
    
    /**
     * 创建分类。
     *
     * @param request 分类创建参数
     */
    void createCategory(AdminCategoryRequest request);
    
    /**
     * 更新分类。
     *
     * @param id 分类 ID
     * @param request 分类更新参数
     */
    void updateCategory(Long id, AdminCategoryRequest request);
    
    /**
     * 删除分类。
     *
     * @param id 分类 ID
     */
    void deleteCategory(Long id);
}

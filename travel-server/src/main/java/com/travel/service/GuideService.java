package com.travel.service;

import com.travel.common.result.PageResult;
import com.travel.dto.guide.*;

import java.util.List;

/**
 * 攻略服务接口
 */
public interface GuideService {
    
    /**
     * 获取攻略列表（用户端）
     */
    PageResult<GuideListResponse> getGuideList(GuideListRequest request);
    
    /**
     * 获取攻略详情
     */
    GuideDetailResponse getGuideDetail(Long guideId);
    
    /**
     * 获取攻略分类列表
     */
    List<String> getCategories();
    
    /**
     * 获取攻略列表（管理端）
     */
    PageResult<AdminGuideListResponse> getAdminGuideList(AdminGuideListRequest request);
    
    /**
     * 获取攻略详情（管理端）
     */
    AdminGuideRequest getAdminGuideDetail(Long guideId);
    
    /**
     * 创建攻略
     */
    Long createGuide(AdminGuideRequest request, Long adminId);
    
    /**
     * 更新攻略
     */
    void updateGuide(Long guideId, AdminGuideRequest request);
    
    /**
     * 更新发布状态
     */
    void updatePublishStatus(Long guideId, Boolean published);
    
    /**
     * 删除攻略
     */
    void deleteGuide(Long guideId);
}

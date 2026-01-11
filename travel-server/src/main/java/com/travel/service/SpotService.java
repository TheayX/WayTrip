package com.travel.service;

import com.travel.common.result.PageResult;
import com.travel.dto.spot.*;

/**
 * 景点服务接口
 */
public interface SpotService {
    
    /**
     * 获取景点列表（用户端）
     */
    PageResult<SpotListResponse> getSpotList(SpotListRequest request);
    
    /**
     * 搜索景点
     */
    PageResult<SpotListResponse> searchSpots(String keyword, Integer page, Integer pageSize);
    
    /**
     * 获取景点详情
     */
    SpotDetailResponse getSpotDetail(Long spotId, Long userId);
    
    /**
     * 获取筛选选项
     */
    SpotFilterResponse getFilters();
    
    /**
     * 获取景点列表（管理端）
     */
    PageResult<AdminSpotListResponse> getAdminSpotList(AdminSpotListRequest request);
    
    /**
     * 获取景点详情（管理端）
     */
    AdminSpotRequest getAdminSpotDetail(Long spotId);
    
    /**
     * 创建景点
     */
    Long createSpot(AdminSpotRequest request);
    
    /**
     * 更新景点
     */
    void updateSpot(Long spotId, AdminSpotRequest request);
    
    /**
     * 更新发布状态
     */
    void updatePublishStatus(Long spotId, Boolean published);
    
    /**
     * 删除景点
     */
    void deleteSpot(Long spotId);
}

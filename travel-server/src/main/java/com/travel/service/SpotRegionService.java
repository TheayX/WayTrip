package com.travel.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.travel.dto.region.AdminRegionRequest;
import com.travel.dto.region.AdminRegionResponse;
import com.travel.entity.SpotRegion;

import java.util.List;

public interface SpotRegionService extends IService<SpotRegion> {
    
    /**
     * 根据父级ID获取地区列表
     */
    List<AdminRegionResponse> getAdminRegions(Long parentId);
    
    /**
     * 创建地区
     */
    void createRegion(AdminRegionRequest request);
    
    /**
     * 更新地区
     */
    void updateRegion(Long id, AdminRegionRequest request);
    
    /**
     * 删除地区
     */
    void deleteRegion(Long id);
}

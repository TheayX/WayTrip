package com.travel.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.travel.dto.region.AdminRegionRequest;
import com.travel.dto.region.AdminRegionResponse;
import com.travel.entity.SpotRegion;

import java.util.List;

/**
 * 景点地区服务接口。
 * <p>
 * 定义管理端地区树查询、创建、更新和删除能力。
 */
public interface SpotRegionService extends IService<SpotRegion> {
    
    /**
     * 根据父级 ID 获取地区列表。
     *
     * @param parentId 父级地区 ID
     * @return 地区列表
     */
    List<AdminRegionResponse> getAdminRegions(Long parentId);
    
    /**
     * 创建地区。
     *
     * @param request 地区创建参数
     */
    void createRegion(AdminRegionRequest request);
    
    /**
     * 更新地区。
     *
     * @param id 地区 ID
     * @param request 地区更新参数
     */
    void updateRegion(Long id, AdminRegionRequest request);
    
    /**
     * 删除地区。
     *
     * @param id 地区 ID
     */
    void deleteRegion(Long id);
}

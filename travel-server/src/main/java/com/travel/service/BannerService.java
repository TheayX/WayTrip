package com.travel.service;

import com.travel.dto.banner.*;

/**
 * 轮播图服务接口
 */
public interface BannerService {

    /**
     * 获取轮播图列表（用户端）
     */
    BannerResponse getBanners();

    /**
     * 获取轮播图列表（管理端）
     */
    AdminBannerListResponse getAdminBanners();

    /**
     * 创建轮播图
     */
    void createBanner(AdminBannerRequest request);

    /**
     * 更新轮播图
     */
    void updateBanner(Long id, AdminBannerRequest request);

    /**
     * 删除轮播图
     */
    void deleteBanner(Long id);

    /**
     * 切换启用状态
     */
    void toggleEnabled(Long id);
}

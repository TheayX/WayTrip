package com.travel.service;

import com.travel.dto.banner.request.AdminBannerRequest;
import com.travel.dto.banner.response.AdminBannerListResponse;
import com.travel.dto.banner.response.BannerResponse;

/**
 * 景点轮播图服务接口。
 * <p>
 * 定义用户端轮播图查询和管理端轮播图维护能力。
 */
public interface SpotBannerService {

    /**
     * 获取用户端轮播图列表。
     *
     * @return 用户端轮播图结果
     */
    BannerResponse getBanners();

    /**
     * 获取管理端轮播图列表。
     *
     * @return 管理端轮播图结果
     */
    AdminBannerListResponse getAdminBanners();

    /**
     * 创建轮播图。
     *
     * @param request 轮播图创建参数
     */
    void createBanner(AdminBannerRequest request);

    /**
     * 更新轮播图。
     *
     * @param id 轮播图 ID
     * @param request 轮播图更新参数
     */
    void updateBanner(Long id, AdminBannerRequest request);

    /**
     * 删除轮播图。
     *
     * @param id 轮播图 ID
     */
    void deleteBanner(Long id);

    /**
     * 切换轮播图启用状态。
     *
     * @param id 轮播图 ID
     */
    void toggleEnabled(Long id);
}


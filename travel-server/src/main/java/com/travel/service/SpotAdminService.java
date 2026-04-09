package com.travel.service;

import com.travel.common.result.PageResult;
import com.travel.dto.spot.request.AdminSpotListRequest;
import com.travel.dto.spot.request.AdminSpotUpsertRequest;
import com.travel.dto.spot.response.AdminSpotDetailResponse;
import com.travel.dto.spot.response.AdminSpotListResponse;

/**
 * 景点管理服务，负责后台列表与维护操作。
 * <p>
 * 后台景点写能力与前台查询能力拆开，避免发布、上下架等后台语义侵入公开查询接口。
 */
public interface SpotAdminService {

    /**
     * 分页查询后台景点列表。
     *
     * @param request 后台筛选参数
     * @return 分页结果
     */
    PageResult<AdminSpotListResponse> getAdminSpotList(AdminSpotListRequest request);

    /**
     * 获取后台景点详情。
     *
     * @param spotId 景点 ID
     * @return 后台详情
     */
    AdminSpotDetailResponse getAdminSpotDetail(Long spotId);

    /**
     * 创建景点。
     *
     * @param request 景点表单
     * @return 新景点 ID
     */
    Long createSpot(AdminSpotUpsertRequest request);

    /**
     * 更新景点。
     *
     * @param spotId 景点 ID
     * @param request 景点表单
     */
    void updateSpot(Long spotId, AdminSpotUpsertRequest request);

    /**
     * 更新景点发布状态。
     *
     * @param spotId 景点 ID
     * @param published 是否发布
     */
    void updatePublishStatus(Long spotId, Boolean published);

    /**
     * 删除景点。
     *
     * @param spotId 景点 ID
     */
    void deleteSpot(Long spotId);
}

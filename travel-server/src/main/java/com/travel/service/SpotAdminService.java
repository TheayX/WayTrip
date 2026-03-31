package com.travel.service;

import com.travel.common.result.PageResult;
import com.travel.dto.spot.request.AdminSpotListRequest;
import com.travel.dto.spot.request.AdminSpotUpsertRequest;
import com.travel.dto.spot.response.AdminSpotDetailResponse;
import com.travel.dto.spot.response.AdminSpotListResponse;

/**
 * 景点管理服务，负责后台列表与维护操作。
 */
public interface SpotAdminService {

    PageResult<AdminSpotListResponse> getAdminSpotList(AdminSpotListRequest request);

    AdminSpotDetailResponse getAdminSpotDetail(Long spotId);

    Long createSpot(AdminSpotUpsertRequest request);

    void updateSpot(Long spotId, AdminSpotUpsertRequest request);

    void updatePublishStatus(Long spotId, Boolean published);

    void deleteSpot(Long spotId);
}

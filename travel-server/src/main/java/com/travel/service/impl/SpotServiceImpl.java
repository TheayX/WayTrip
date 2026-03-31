package com.travel.service.impl;

import com.travel.common.result.PageResult;
import com.travel.dto.spot.request.AdminSpotListRequest;
import com.travel.dto.spot.request.AdminSpotUpsertRequest;
import com.travel.dto.spot.request.SpotListRequest;
import com.travel.dto.spot.response.AdminSpotDetailResponse;
import com.travel.dto.spot.response.AdminSpotListResponse;
import com.travel.dto.spot.response.SpotDetailResponse;
import com.travel.dto.spot.response.SpotFilterResponse;
import com.travel.dto.spot.response.SpotListResponse;
import com.travel.dto.spot.response.SpotViewHistoryResponse;
import com.travel.service.SpotAdminService;
import com.travel.service.SpotBehaviorService;
import com.travel.service.SpotHeatService;
import com.travel.service.SpotQueryService;
import com.travel.service.SpotService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 景点服务门面，统一承接 controller 调用并分发到具体子服务。
 */
@Service
@RequiredArgsConstructor
public class SpotServiceImpl implements SpotService {

    private final SpotQueryService spotQueryService;
    private final SpotAdminService spotAdminService;
    private final SpotBehaviorService spotBehaviorService;
    private final SpotHeatService spotHeatService;

    @Override
    public PageResult<SpotListResponse> getSpotList(SpotListRequest request) {
        return spotQueryService.getSpotList(request);
    }

    @Override
    public PageResult<SpotListResponse> searchSpots(String keyword, Integer page, Integer pageSize) {
        return spotQueryService.searchSpots(keyword, page, pageSize);
    }

    @Override
    public PageResult<SpotViewHistoryResponse> getViewHistory(Long userId, Integer page, Integer pageSize) {
        return spotQueryService.getViewHistory(userId, page, pageSize);
    }

    @Override
    public SpotDetailResponse getSpotDetail(Long spotId, Long userId) {
        return spotQueryService.getSpotDetail(spotId, userId);
    }

    @Override
    public void recordView(Long spotId, Long userId, String source, Integer duration) {
        spotBehaviorService.recordView(spotId, userId, source, duration);
    }

    @Override
    public SpotFilterResponse getFilters() {
        return spotQueryService.getFilters();
    }

    @Override
    public PageResult<AdminSpotListResponse> getAdminSpotList(AdminSpotListRequest request) {
        return spotAdminService.getAdminSpotList(request);
    }

    @Override
    public AdminSpotDetailResponse getAdminSpotDetail(Long spotId) {
        return spotAdminService.getAdminSpotDetail(spotId);
    }

    @Override
    public Long createSpot(AdminSpotUpsertRequest request) {
        return spotAdminService.createSpot(request);
    }

    @Override
    public void updateSpot(Long spotId, AdminSpotUpsertRequest request) {
        spotAdminService.updateSpot(spotId, request);
    }

    @Override
    public void updatePublishStatus(Long spotId, Boolean published) {
        spotAdminService.updatePublishStatus(spotId, published);
    }

    @Override
    public void deleteSpot(Long spotId) {
        spotAdminService.deleteSpot(spotId);
    }

    @Override
    public void refreshSpotHeat(Long spotId) {
        spotHeatService.refreshSpotHeat(spotId);
    }

    @Override
    public void refreshAllSpotHeat() {
        spotHeatService.refreshAllSpotHeat();
    }
}

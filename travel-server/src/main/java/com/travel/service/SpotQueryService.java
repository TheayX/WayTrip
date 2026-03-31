package com.travel.service;

import com.travel.common.result.PageResult;
import com.travel.dto.spot.request.SpotListRequest;
import com.travel.dto.spot.response.SpotDetailResponse;
import com.travel.dto.spot.response.SpotFilterResponse;
import com.travel.dto.spot.response.SpotListResponse;
import com.travel.dto.spot.response.SpotViewHistoryResponse;

/**
 * 景点查询服务，负责用户端列表、详情和筛选。
 */
public interface SpotQueryService {

    PageResult<SpotListResponse> getSpotList(SpotListRequest request);

    PageResult<SpotListResponse> searchSpots(String keyword, Integer page, Integer pageSize);

    PageResult<SpotViewHistoryResponse> getViewHistory(Long userId, Integer page, Integer pageSize);

    SpotDetailResponse getSpotDetail(Long spotId, Long userId);

    SpotFilterResponse getFilters();
}

package com.travel.service;

import com.travel.common.result.PageResult;
import com.travel.dto.spot.request.SpotListRequest;
import com.travel.dto.spot.response.SpotDetailResponse;
import com.travel.dto.spot.response.SpotFilterResponse;
import com.travel.dto.spot.response.SpotListResponse;
import com.travel.dto.spot.response.SpotViewHistoryResponse;

/**
 * 景点查询服务，负责用户端列表、详情和筛选。
 * <p>
 * 面向用户端的景点只读能力统一从这里进入，保证发布态与可见性规则一致。
 */
public interface SpotQueryService {

    /**
     * 分页查询景点列表。
     *
     * @param request 列表筛选参数
     * @return 分页结果
     */
    PageResult<SpotListResponse> getSpotList(SpotListRequest request);

    /**
     * 关键词搜索景点。
     *
     * @param keyword 关键词
     * @param page 页码
     * @param pageSize 每页条数
     * @return 分页结果
     */
    PageResult<SpotListResponse> searchSpots(String keyword, Integer page, Integer pageSize);

    /**
     * 查询用户浏览历史。
     *
     * @param userId 用户 ID
     * @param page 页码
     * @param pageSize 每页条数
     * @return 分页结果
     */
    PageResult<SpotViewHistoryResponse> getViewHistory(Long userId, Integer page, Integer pageSize);

    /**
     * 获取景点详情。
     *
     * @param spotId 景点 ID
     * @param userId 当前用户 ID，可为空
     * @return 景点详情
     */
    SpotDetailResponse getSpotDetail(Long spotId, Long userId);

    /**
     * 获取筛选项集合。
     *
     * @return 筛选项数据
     */
    SpotFilterResponse getFilters();
}

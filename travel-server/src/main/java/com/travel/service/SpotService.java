package com.travel.service;

import com.travel.common.result.PageResult;
import com.travel.dto.spot.*;

/**
 * 景点服务接口。
 * <p>
 * 定义用户端景点浏览、搜索、筛选，以及管理端景点维护和热度同步能力。
 */
public interface SpotService {
    
    /**
     * 分页获取用户端景点列表。
     *
     * @param request 景点查询参数
     * @return 景点分页结果
     */
    PageResult<SpotListResponse> getSpotList(SpotListRequest request);
    
    /**
     * 按关键词搜索景点。
     *
     * @param keyword 搜索关键词
     * @param page 当前页码
     * @param pageSize 每页条数
     * @return 景点分页结果
     */
    PageResult<SpotListResponse> searchSpots(String keyword, Integer page, Integer pageSize);
    
    /**
     * 获取景点详情。
     *
     * @param spotId 景点 ID
     * @param userId 当前登录用户 ID，可为空
     * @return 景点详情
     */
    SpotDetailResponse getSpotDetail(Long spotId, Long userId);

    /**
     * 记录用户浏览行为。
     *
     * @param spotId 景点 ID
     * @param userId 当前登录用户 ID，可为空
     * @param source 浏览来源
     * @param duration 停留时长（秒）
     */
    void recordView(Long spotId, Long userId, String source, Integer duration);
    
    /**
     * 获取景点筛选项。
     *
     * @return 景点筛选项结果
     */
    SpotFilterResponse getFilters();
    
    /**
     * 分页获取管理端景点列表。
     *
     * @param request 管理端景点查询参数
     * @return 管理端景点分页结果
     */
    PageResult<AdminSpotListResponse> getAdminSpotList(AdminSpotListRequest request);
    
    /**
     * 获取管理端景点详情。
     *
     * @param spotId 景点 ID
     * @return 管理端景点详情
     */
    AdminSpotDetailResponse getAdminSpotDetail(Long spotId);
    
    /**
     * 创建景点。
     *
     * @param request 景点创建参数
     * @return 新创建景点 ID
     */
    Long createSpot(AdminSpotUpsertRequest request);
    
    /**
     * 更新景点信息。
     *
     * @param spotId 景点 ID
     * @param request 景点更新参数
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

    /**
     * 按热度档位和行为数据同步单个景点热度。
     *
     * @param spotId 景点 ID
     */
    void refreshSpotHeat(Long spotId);

    /**
     * 按热度档位和行为数据同步全部景点热度。
     */
    void refreshAllSpotHeat();
}

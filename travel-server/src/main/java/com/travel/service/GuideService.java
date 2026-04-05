package com.travel.service;

import com.travel.common.result.PageResult;
import com.travel.dto.guide.request.AdminGuideListRequest;
import com.travel.dto.guide.request.AdminGuideRequest;
import com.travel.dto.guide.request.AdminGuideViewCountRequest;
import com.travel.dto.guide.request.GuideBudgetListRequest;
import com.travel.dto.guide.request.GuideListRequest;
import com.travel.dto.guide.response.AdminGuideListResponse;
import com.travel.dto.guide.response.GuideBudgetListResponse;
import com.travel.dto.guide.response.GuideDetailResponse;
import com.travel.dto.guide.response.GuideListResponse;

import java.util.List;

/**
 * 攻略服务接口。
 * <p>
 * 定义用户端攻略浏览，以及管理端攻略维护能力。
 */
public interface GuideService {
    
    /**
     * 分页获取用户端攻略列表。
     *
     * @param request 攻略查询参数
     * @return 攻略分页结果
     */
    PageResult<GuideListResponse> getGuideList(GuideListRequest request);

    /**
     * 分页获取穷游攻略列表。
     *
     * @param request 穷游攻略查询参数
     * @return 穷游攻略分页结果
     */
    PageResult<GuideBudgetListResponse> getBudgetGuideList(GuideBudgetListRequest request);
    
    /**
     * 获取攻略详情。
     *
     * @param guideId 攻略 ID
     * @return 攻略详情
     */
    GuideDetailResponse getGuideDetail(Long guideId);
    
    /**
     * 获取攻略分类列表。
     *
     * @return 攻略分类集合
     */
    List<String> getCategories();
    
    /**
     * 分页获取管理端攻略列表。
     *
     * @param request 管理端攻略查询参数
     * @return 管理端攻略分页结果
     */
    PageResult<AdminGuideListResponse> getAdminGuideList(AdminGuideListRequest request);
    
    /**
     * 获取管理端攻略详情。
     *
     * @param guideId 攻略 ID
     * @return 管理端攻略详情
     */
    AdminGuideRequest getAdminGuideDetail(Long guideId);
    
    /**
     * 创建攻略。
     *
     * @param request 攻略创建参数
     * @param adminId 当前管理员 ID
     * @return 新创建攻略 ID
     */
    Long createGuide(AdminGuideRequest request, Long adminId);
    
    /**
     * 更新攻略。
     *
     * @param guideId 攻略 ID
     * @param request 攻略更新参数
     */
    void updateGuide(Long guideId, AdminGuideRequest request);

    /**
     * 单独更新攻略浏览量。
     *
     * @param guideId 攻略 ID
     * @param request 浏览量更新参数
     */
    void updateGuideViewCount(Long guideId, AdminGuideViewCountRequest request);
    
    /**
     * 更新攻略发布状态。
     *
     * @param guideId 攻略 ID
     * @param published 是否发布
     */
    void updatePublishStatus(Long guideId, Boolean published);
    
    /**
     * 删除攻略。
     *
     * @param guideId 攻略 ID
     */
    void deleteGuide(Long guideId);
}

package com.travel.service;

import com.travel.common.result.PageResult;
import com.travel.dto.guide.request.AdminGuideListRequest;
import com.travel.dto.guide.request.AdminGuideRequest;
import com.travel.dto.guide.request.AdminGuideViewCountRequest;
import com.travel.dto.guide.response.AdminGuideListResponse;

/**
 * 攻略管理服务，负责后台侧的查询与维护能力。
 * <p>
 * 该接口只面向管理端，和用户端只读查询能力拆分，避免后台写逻辑污染前台查询链路。
 */
public interface GuideAdminService {

    /**
     * 分页查询后台攻略列表。
     *
     * @param request 后台攻略筛选参数
     * @return 分页结果
     */
    PageResult<AdminGuideListResponse> getAdminGuideList(AdminGuideListRequest request);

    /**
     * 获取后台攻略编辑详情。
     *
     * @param guideId 攻略 ID
     * @return 后台攻略详情
     */
    AdminGuideRequest getAdminGuideDetail(Long guideId);

    /**
     * 创建攻略。
     *
     * @param request 攻略表单
     * @param adminId 创建管理员 ID
     * @return 新攻略 ID
     */
    Long createGuide(AdminGuideRequest request, Long adminId);

    /**
     * 更新攻略内容。
     *
     * @param guideId 攻略 ID
     * @param request 攻略表单
     */
    void updateGuide(Long guideId, AdminGuideRequest request);

    /**
     * 更新攻略浏览量。
     *
     * @param guideId 攻略 ID
     * @param request 浏览量参数
     */
    void updateGuideViewCount(Long guideId, AdminGuideViewCountRequest request);

    /**
     * 更新发布状态。
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

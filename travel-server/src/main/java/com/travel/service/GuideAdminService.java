package com.travel.service;

import com.travel.common.result.PageResult;
import com.travel.dto.guide.request.AdminGuideListRequest;
import com.travel.dto.guide.request.AdminGuideRequest;
import com.travel.dto.guide.request.AdminGuideViewCountRequest;
import com.travel.dto.guide.response.AdminGuideListResponse;

/**
 * 攻略管理服务，负责后台侧的查询与维护能力。
 */
public interface GuideAdminService {

    PageResult<AdminGuideListResponse> getAdminGuideList(AdminGuideListRequest request);

    AdminGuideRequest getAdminGuideDetail(Long guideId);

    Long createGuide(AdminGuideRequest request, Long adminId);

    void updateGuide(Long guideId, AdminGuideRequest request);

    void updateGuideViewCount(Long guideId, AdminGuideViewCountRequest request);

    void updatePublishStatus(Long guideId, Boolean published);

    void deleteGuide(Long guideId);
}

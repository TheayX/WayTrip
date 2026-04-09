package com.travel.service.impl;

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
import com.travel.service.GuideAdminService;
import com.travel.service.GuideQueryService;
import com.travel.service.GuideService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 攻略服务门面，统一承接 controller 调用并分发到具体子服务。
 * <p>
 * 通过门面把用户端查询与后台管理隔开，对外仍保持一套稳定的 GuideService 接口。
 */
@Service
@RequiredArgsConstructor
public class GuideServiceImpl implements GuideService {

    // 查询与管理两类子服务分层注入，保证后续扩展不再回到“大接口大实现”的结构。

    private final GuideQueryService guideQueryService;
    private final GuideAdminService guideAdminService;

    /**
     * 门面层只负责分发，避免再次把查询细节回流到统一服务里。
     */
    @Override
    public PageResult<GuideListResponse> getGuideList(GuideListRequest request) {
        return guideQueryService.getGuideList(request);
    }

    @Override
    public PageResult<GuideBudgetListResponse> getBudgetGuideList(GuideBudgetListRequest request) {
        return guideQueryService.getBudgetGuideList(request);
    }

    @Override
    public GuideDetailResponse getGuideDetail(Long guideId) {
        return guideQueryService.getGuideDetail(guideId);
    }

    @Override
    public List<String> getCategories() {
        return guideQueryService.getCategories();
    }

    @Override
    public PageResult<AdminGuideListResponse> getAdminGuideList(AdminGuideListRequest request) {
        return guideAdminService.getAdminGuideList(request);
    }

    @Override
    public AdminGuideRequest getAdminGuideDetail(Long guideId) {
        return guideAdminService.getAdminGuideDetail(guideId);
    }

    @Override
    public Long createGuide(AdminGuideRequest request, Long adminId) {
        return guideAdminService.createGuide(request, adminId);
    }

    @Override
    public void updateGuide(Long guideId, AdminGuideRequest request) {
        guideAdminService.updateGuide(guideId, request);
    }

    @Override
    public void updateGuideViewCount(Long guideId, AdminGuideViewCountRequest request) {
        guideAdminService.updateGuideViewCount(guideId, request);
    }

    @Override
    public void updatePublishStatus(Long guideId, Boolean published) {
        guideAdminService.updatePublishStatus(guideId, published);
    }

    @Override
    public void deleteGuide(Long guideId) {
        guideAdminService.deleteGuide(guideId);
    }
}

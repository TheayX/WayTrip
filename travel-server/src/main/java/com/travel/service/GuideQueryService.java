package com.travel.service;

import com.travel.common.result.PageResult;
import com.travel.dto.guide.request.GuideBudgetListRequest;
import com.travel.dto.guide.request.GuideListRequest;
import com.travel.dto.guide.response.GuideBudgetListResponse;
import com.travel.dto.guide.response.GuideDetailResponse;
import com.travel.dto.guide.response.GuideListResponse;

import java.util.List;

/**
 * 攻略查询服务，负责用户端和公共查询能力。
 */
public interface GuideQueryService {

    PageResult<GuideListResponse> getGuideList(GuideListRequest request);

    PageResult<GuideBudgetListResponse> getBudgetGuideList(GuideBudgetListRequest request);

    GuideDetailResponse getGuideDetail(Long guideId);

    List<String> getCategories();
}

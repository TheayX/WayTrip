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
 * <p>
 * 所有只读攻略查询都从这里进入，便于后续统一做缓存、排序和发布态校验。
 */
public interface GuideQueryService {

    /**
     * 分页查询攻略列表。
     *
     * @param request 列表筛选参数
     * @return 分页结果
     */
    PageResult<GuideListResponse> getGuideList(GuideListRequest request);

    /**
     * 分页查询穷游攻略列表。
     *
     * @param request 穷游筛选参数
     * @return 分页结果
     */
    PageResult<GuideBudgetListResponse> getBudgetGuideList(GuideBudgetListRequest request);

    /**
     * 获取已发布攻略详情。
     *
     * @param guideId 攻略 ID
     * @return 攻略详情
     */
    GuideDetailResponse getGuideDetail(Long guideId);

    /**
     * 获取攻略分类集合。
     *
     * @return 分类列表
     */
    List<String> getCategories();
}

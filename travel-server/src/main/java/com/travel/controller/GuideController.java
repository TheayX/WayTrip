package com.travel.controller;

import com.travel.common.result.ApiResponse;
import com.travel.common.result.PageResult;
import com.travel.dto.guide.*;
import com.travel.service.GuideService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户端攻略接口
 */
@Tag(name = "攻略")
@RestController
@RequestMapping("/api/v1/guides")
@RequiredArgsConstructor
public class GuideController {

    private final GuideService guideService;

    @Operation(summary = "获取攻略列表")
    @GetMapping
    public ApiResponse<PageResult<GuideListResponse>> getGuideList(GuideListRequest request) {
        return ApiResponse.success(guideService.getGuideList(request));
    }

    @Operation(summary = "获取攻略详情")
    @GetMapping("/{guideId}")
    public ApiResponse<GuideDetailResponse> getGuideDetail(@PathVariable Long guideId) {
        return ApiResponse.success(guideService.getGuideDetail(guideId));
    }

    @Operation(summary = "获取攻略分类")
    @GetMapping("/categories")
    public ApiResponse<List<String>> getCategories() {
        return ApiResponse.success(guideService.getCategories());
    }
}

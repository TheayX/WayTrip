package com.travel.controller.admin;

import com.travel.common.result.ApiResponse;
import com.travel.common.result.PageResult;
import com.travel.dto.guide.*;
import com.travel.service.GuideService;
import com.travel.util.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

/**
 * 管理端攻略接口
 */
@Tag(name = "攻略管理")
@RestController
@RequestMapping("/api/admin/v1/guides")
@RequiredArgsConstructor
public class AdminGuideController {

    private final GuideService guideService;

    @Operation(summary = "获取攻略列表")
    @GetMapping
    public ApiResponse<PageResult<AdminGuideListResponse>> getGuideList(AdminGuideListRequest request) {
        return ApiResponse.success(guideService.getAdminGuideList(request));
    }

    @Operation(summary = "获取攻略详情")
    @GetMapping("/{guideId}")
    public ApiResponse<AdminGuideRequest> getGuideDetail(@PathVariable Long guideId) {
        return ApiResponse.success(guideService.getAdminGuideDetail(guideId));
    }

    @Operation(summary = "获取攻略分类")
    @GetMapping("/categories")
    public ApiResponse<List<String>> getCategories() {
        return ApiResponse.success(guideService.getCategories());
    }

    @Operation(summary = "创建攻略")
    @PostMapping
    public ApiResponse<Map<String, Long>> createGuide(@Valid @RequestBody AdminGuideRequest request) {
        Long adminId = UserContext.getAdminId();
        Long id = guideService.createGuide(request, adminId);
        return ApiResponse.success(Map.of("id", id));
    }

    @Operation(summary = "更新攻略")
    @PutMapping("/{guideId}")
    public ApiResponse<Void> updateGuide(@PathVariable Long guideId, @RequestBody AdminGuideRequest request) {
        guideService.updateGuide(guideId, request);
        return ApiResponse.success();
    }

    @Operation(summary = "更新发布状态")
    @PutMapping("/{guideId}/publish")
    public ApiResponse<Void> updatePublishStatus(@PathVariable Long guideId, @RequestBody Map<String, Boolean> body) {
        guideService.updatePublishStatus(guideId, body.get("published"));
        return ApiResponse.success();
    }

    @Operation(summary = "删除攻略")
    @DeleteMapping("/{guideId}")
    public ApiResponse<Void> deleteGuide(@PathVariable Long guideId) {
        guideService.deleteGuide(guideId);
        return ApiResponse.success();
    }
}

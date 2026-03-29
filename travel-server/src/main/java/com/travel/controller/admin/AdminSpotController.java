package com.travel.controller.admin;

import com.travel.common.result.ApiResponse;
import com.travel.common.result.PageResult;
import com.travel.dto.spot.*;
import com.travel.service.ReviewService;
import com.travel.service.SpotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.Map;

/**
 * 管理端景点控制器，负责景点管理与数据刷新接口。
 */
@Tag(name = "管理端-景点", description = "管理端景点管理相关接口")
@RestController
@RequestMapping("/api/admin/v1/spots")
@RequiredArgsConstructor
public class AdminSpotController {

    private final SpotService spotService;
    private final ReviewService reviewService;

    @Operation(summary = "获取景点列表")
    @GetMapping
    public ApiResponse<PageResult<AdminSpotListResponse>> getSpotList(AdminSpotListRequest request) {
        return ApiResponse.success(spotService.getAdminSpotList(request));
    }

    @Operation(summary = "获取景点详情")
    @GetMapping("/{spotId}")
    public ApiResponse<AdminSpotDetailResponse> getSpotDetail(@PathVariable("spotId") Long spotId) {
        return ApiResponse.success(spotService.getAdminSpotDetail(spotId));
    }

    @Operation(summary = "创建景点")
    @PostMapping
    public ApiResponse<Map<String, Long>> createSpot(@Valid @RequestBody AdminSpotUpsertRequest request) {
        Long id = spotService.createSpot(request);
        return ApiResponse.success(Map.of("id", id));
    }

    @Operation(summary = "更新景点")
    @PutMapping("/{spotId}")
    public ApiResponse<Void> updateSpot(@PathVariable("spotId") Long spotId, @Valid @RequestBody AdminSpotUpsertRequest request) {
        spotService.updateSpot(spotId, request);
        return ApiResponse.success();
    }

    @Operation(summary = "更新发布状态")
    @PutMapping("/{spotId}/publish")
    public ApiResponse<Void> updatePublishStatus(@PathVariable("spotId") Long spotId, @RequestBody Map<String, Boolean> body) {
        spotService.updatePublishStatus(spotId, body.get("published"));
        return ApiResponse.success();
    }

    @Operation(summary = "删除景点")
    @DeleteMapping("/{spotId}")
    public ApiResponse<Void> deleteSpot(@PathVariable("spotId") Long spotId) {
        spotService.deleteSpot(spotId);
        return ApiResponse.success();
    }

    @Operation(summary = "按评价表重算单个景点评分")
    @PostMapping("/{spotId}/rating/refresh")
    public ApiResponse<Void> refreshSpotRating(@PathVariable("spotId") Long spotId) {
        reviewService.refreshSpotRating(spotId);
        return ApiResponse.success();
    }

    @Operation(summary = "按评价表重算全部景点评分")
    @PostMapping("/rating/refresh")
    public ApiResponse<Void> refreshAllSpotRatings() {
        reviewService.refreshAllSpotRatings();
        return ApiResponse.success();
    }

    @Operation(summary = "按热度档位和行为数据同步单个景点热度")
    @PostMapping("/{spotId}/heat/refresh")
    public ApiResponse<Void> refreshSpotHeat(@PathVariable("spotId") Long spotId) {
        spotService.refreshSpotHeat(spotId);
        return ApiResponse.success();
    }

    @Operation(summary = "按热度档位和行为数据同步全部景点热度")
    @PostMapping("/heat/refresh")
    public ApiResponse<Void> refreshAllSpotHeat() {
        spotService.refreshAllSpotHeat();
        return ApiResponse.success();
    }

    @Operation(summary = "获取筛选选项（地区、分类）")
    @GetMapping("/filters")
    public ApiResponse<SpotFilterResponse> getFilters() {
        return ApiResponse.success(spotService.getFilters());
    }
}

package com.travel.controller.admin;

import com.travel.common.result.ApiResponse;
import com.travel.common.result.PageResult;
import com.travel.dto.spot.*;
import com.travel.service.SpotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.Map;

/**
 * 管理端景点接口
 */
@Tag(name = "景点管理")
@RestController
@RequestMapping("/api/admin/v1/spots")
@RequiredArgsConstructor
public class AdminSpotController {

    private final SpotService spotService;

    @Operation(summary = "获取景点列表")
    @GetMapping
    public ApiResponse<PageResult<AdminSpotListResponse>> getSpotList(AdminSpotListRequest request) {
        return ApiResponse.success(spotService.getAdminSpotList(request));
    }

    @Operation(summary = "获取景点详情")
    @GetMapping("/{spotId}")
    public ApiResponse<AdminSpotRequest> getSpotDetail(@PathVariable Long spotId) {
        return ApiResponse.success(spotService.getAdminSpotDetail(spotId));
    }

    @Operation(summary = "创建景点")
    @PostMapping
    public ApiResponse<Map<String, Long>> createSpot(@Valid @RequestBody AdminSpotRequest request) {
        Long id = spotService.createSpot(request);
        return ApiResponse.success(Map.of("id", id));
    }

    @Operation(summary = "更新景点")
    @PutMapping("/{spotId}")
    public ApiResponse<Void> updateSpot(@PathVariable Long spotId, @RequestBody AdminSpotRequest request) {
        spotService.updateSpot(spotId, request);
        return ApiResponse.success();
    }

    @Operation(summary = "更新发布状态")
    @PutMapping("/{spotId}/publish")
    public ApiResponse<Void> updatePublishStatus(@PathVariable Long spotId, @RequestBody Map<String, Boolean> body) {
        spotService.updatePublishStatus(spotId, body.get("published"));
        return ApiResponse.success();
    }

    @Operation(summary = "删除景点")
    @DeleteMapping("/{spotId}")
    public ApiResponse<Void> deleteSpot(@PathVariable Long spotId) {
        spotService.deleteSpot(spotId);
        return ApiResponse.success();
    }
}

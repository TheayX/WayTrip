package com.travel.controller.app;

import com.travel.common.result.ApiResponse;
import com.travel.common.result.PageResult;
import com.travel.dto.spot.*;
import com.travel.service.SpotService;
import com.travel.util.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 用户端景点控制器，负责景点浏览、搜索与行为上报接口。
 */
@Tag(name = "用户端-景点", description = "用户端景点浏览与搜索相关接口")
@RestController
@RequestMapping("/api/v1/spots")
@RequiredArgsConstructor
public class SpotController {

    private final SpotService spotService;

    @Operation(summary = "获取景点列表")
    @GetMapping
    public ApiResponse<PageResult<SpotListResponse>> getSpotList(SpotListRequest request) {
        return ApiResponse.success(spotService.getSpotList(request));
    }

    @Operation(summary = "搜索景点")
    @GetMapping("/search")
    public ApiResponse<PageResult<SpotListResponse>> searchSpots(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return ApiResponse.success(spotService.searchSpots(keyword, page, pageSize));
    }

    @Operation(summary = "获取景点详情")
    @GetMapping("/{spotId}")
    public ApiResponse<SpotDetailResponse> getSpotDetail(@PathVariable("spotId") Long spotId) {
        Long userId = UserContext.getUserId();
        return ApiResponse.success(spotService.getSpotDetail(spotId, userId));
    }

    @Operation(summary = "上报浏览行为")
    @PostMapping("/{spotId}/view")
    public ApiResponse<Void> recordView(
            @PathVariable("spotId") Long spotId,
            @RequestParam(defaultValue = "detail") String source,
            @RequestParam(defaultValue = "0") Integer duration) {
        Long userId = UserContext.getUserId();
        spotService.recordView(spotId, userId, source, duration);
        return ApiResponse.success(null);
    }

    @Operation(summary = "获取筛选选项")
    @GetMapping("/filters")
    public ApiResponse<SpotFilterResponse> getFilters() {
        return ApiResponse.success(spotService.getFilters());
    }
}

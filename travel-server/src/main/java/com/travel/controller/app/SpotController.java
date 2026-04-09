package com.travel.controller.app;

import com.travel.common.result.ApiResponse;
import com.travel.common.result.PageResult;
import com.travel.dto.spot.request.SpotListRequest;
import com.travel.dto.spot.response.SpotDetailResponse;
import com.travel.dto.spot.response.SpotFilterResponse;
import com.travel.dto.spot.response.SpotListResponse;
import com.travel.dto.spot.response.SpotViewHistoryResponse;
import com.travel.service.SpotService;
import com.travel.util.web.UserContextHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 用户端景点控制器，负责景点浏览、搜索与行为上报接口。
 * <p>
 * 这里同时承接公开查询和登录后行为上报，因此需要在接口层显式区分是否依赖用户上下文。
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

    @Operation(summary = "获取我的最近浏览")
    @GetMapping("/views")
    public ApiResponse<PageResult<SpotViewHistoryResponse>> getViewHistory(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        // 浏览历史属于用户私有数据，只能从当前登录上下文读取。
        Long userId = UserContextHolder.getUserId();
        return ApiResponse.success(spotService.getViewHistory(userId, page, pageSize));
    }

    @Operation(summary = "获取景点详情")
    @GetMapping("/{spotId}")
    public ApiResponse<SpotDetailResponse> getSpotDetail(@PathVariable("spotId") Long spotId) {
        Long userId = UserContextHolder.getUserId();
        return ApiResponse.success(spotService.getSpotDetail(spotId, userId));
    }

    @Operation(summary = "上报浏览行为")
    @PostMapping("/{spotId}/view")
    public ApiResponse<Void> recordView(
            @PathVariable("spotId") Long spotId,
            @RequestParam(defaultValue = "detail") String source,
            @RequestParam(defaultValue = "0") Integer duration) {
        // 来源和停留时长直接透传给行为服务，用于后续热度和推荐打分。
        Long userId = UserContextHolder.getUserId();
        spotService.recordView(spotId, userId, source, duration);
        return ApiResponse.success(null);
    }

    @Operation(summary = "获取筛选选项")
    @GetMapping("/filters")
    public ApiResponse<SpotFilterResponse> getFilters() {
        return ApiResponse.success(spotService.getFilters());
    }
}

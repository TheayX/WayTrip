package com.travel.controller.admin;

import com.travel.common.result.ApiResponse;
import com.travel.dto.dashboard.response.DashboardOverviewResponse;
import com.travel.dto.dashboard.response.HotSpotsResponse;
import com.travel.dto.dashboard.response.OrderTrendResponse;
import com.travel.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 管理端仪表板控制器，负责概览统计接口。
 */
@Tag(name = "管理端-仪表板", description = "管理端仪表板统计相关接口")
@RestController
@RequestMapping("/api/admin/v1/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final DashboardService dashboardService;

    @Operation(summary = "获取概览数据")
    @GetMapping("/overview")
    public ApiResponse<DashboardOverviewResponse> getOverview() {
        return ApiResponse.success(dashboardService.getOverview());
    }

    @Operation(summary = "获取订单趋势")
    @GetMapping("/order-trend")
    public ApiResponse<OrderTrendResponse> getOrderTrend(
            @RequestParam(defaultValue = "7") Integer days,
            @RequestParam(defaultValue = "weekday") String mode) {
        return ApiResponse.success(dashboardService.getOrderTrend(days, mode));
    }

    @Operation(summary = "获取热门景点")
    @GetMapping("/hot-spots")
    public ApiResponse<HotSpotsResponse> getHotSpots(
            @RequestParam(defaultValue = "10") Integer limit) {
        return ApiResponse.success(dashboardService.getHotSpots(limit));
    }
}

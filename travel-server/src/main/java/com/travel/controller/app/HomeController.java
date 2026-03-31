package com.travel.controller.app;

import com.travel.common.result.ApiResponse;
import com.travel.dto.banner.response.BannerResponse;
import com.travel.dto.home.response.HotSpotResponse;
import com.travel.dto.home.response.NearbySpotResponse;
import com.travel.dto.home.response.RecentViewedSpotResponse;
import com.travel.service.SpotBannerService;
import com.travel.service.RecommendationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * 用户端首页控制器，负责首页聚合数据接口。
 */
@Tag(name = "用户端-首页", description = "首页轮播图、热门景点相关接口")
@RestController
@RequestMapping("/api/v1/home")
@RequiredArgsConstructor
public class HomeController {

    private final RecommendationService recommendationService;
    private final SpotBannerService spotBannerService;

    @Operation(summary = "获取轮播图")
    @GetMapping("/banners")
    public ApiResponse<BannerResponse> getBanners() {
        return ApiResponse.success(spotBannerService.getBanners());
    }

    @Operation(summary = "获取热门景点")
    @GetMapping("/hot")
    public ApiResponse<HotSpotResponse> getHotSpots(
            @RequestParam(defaultValue = "10") Integer limit) {
        return ApiResponse.success(recommendationService.getHotSpots(limit));
    }

    @Operation(summary = "获取最近都在看")
    @GetMapping("/recent-views")
    public ApiResponse<RecentViewedSpotResponse> getRecentViewedSpots(
            @RequestParam(defaultValue = "14") Integer days,
            @RequestParam(defaultValue = "12") Integer limit) {
        return ApiResponse.success(recommendationService.getRecentViewedSpots(days, limit));
    }

    @Operation(summary = "获取附近景点")
    @GetMapping("/nearby")
    public ApiResponse<NearbySpotResponse> getNearbySpots(
            @RequestParam BigDecimal latitude,
            @RequestParam BigDecimal longitude,
            @RequestParam(defaultValue = "3") Integer limit) {
        return ApiResponse.success(recommendationService.getNearbySpots(latitude, longitude, limit));
    }
}

package com.travel.controller.app;

import com.travel.common.result.ApiResponse;
import com.travel.dto.banner.BannerResponse;
import com.travel.dto.home.HotSpotResponse;
import com.travel.service.SpotBannerService;
import com.travel.service.RecommendationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 用户端首页接口
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
}

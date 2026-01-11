package com.travel.controller;

import com.travel.common.result.ApiResponse;
import com.travel.dto.banner.BannerResponse;
import com.travel.dto.home.HotSpotResponse;
import com.travel.service.BannerService;
import com.travel.service.RecommendationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "首页接口", description = "首页相关接口")
@RestController
@RequestMapping("/api/v1/home")
@RequiredArgsConstructor
public class HomeController {

    private final RecommendationService recommendationService;
    private final BannerService bannerService;

    @Operation(summary = "获取轮播图")
    @GetMapping("/banners")
    public ApiResponse<BannerResponse> getBanners() {
        return ApiResponse.success(bannerService.getBanners());
    }

    @Operation(summary = "获取热门景点")
    @GetMapping("/hot")
    public ApiResponse<HotSpotResponse> getHotSpots(
            @RequestParam(defaultValue = "10") Integer limit) {
        return ApiResponse.success(recommendationService.getHotSpots(limit));
    }
}

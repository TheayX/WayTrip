package com.travel.controller.admin;

import com.travel.common.result.ApiResponse;
import com.travel.dto.banner.*;
import com.travel.service.BannerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "管理端轮播图接口", description = "管理端轮播图管理相关接口")
@RestController
@RequestMapping("/api/admin/v1/banners")
@RequiredArgsConstructor
public class AdminBannerController {

    private final BannerService bannerService;

    @Operation(summary = "获取轮播图列表")
    @GetMapping
    public ApiResponse<AdminBannerListResponse> getBanners() {
        return ApiResponse.success(bannerService.getAdminBanners());
    }

    @Operation(summary = "创建轮播图")
    @PostMapping
    public ApiResponse<Void> createBanner(@Valid @RequestBody AdminBannerRequest request) {
        bannerService.createBanner(request);
        return ApiResponse.success(null);
    }

    @Operation(summary = "更新轮播图")
    @PutMapping("/{id}")
    public ApiResponse<Void> updateBanner(
            @PathVariable("id") Long id,
            @Valid @RequestBody AdminBannerRequest request) {
        bannerService.updateBanner(id, request);
        return ApiResponse.success(null);
    }

    @Operation(summary = "删除轮播图")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteBanner(@PathVariable("id") Long id) {
        bannerService.deleteBanner(id);
        return ApiResponse.success(null);
    }

    @Operation(summary = "切换启用状态")
    @PostMapping("/{id}/toggle")
    public ApiResponse<Void> toggleEnabled(@PathVariable("id") Long id) {
        bannerService.toggleEnabled(id);
        return ApiResponse.success(null);
    }
}

package com.travel.controller.admin;

import com.travel.common.result.ApiResponse;
import com.travel.common.result.PageResult;
import com.travel.dto.user.item.AdminUserFavoriteListItem;
import com.travel.dto.user.item.AdminUserPreferenceListItem;
import com.travel.dto.user.item.AdminUserViewListItem;
import com.travel.dto.user.request.AdminUserFavoriteListRequest;
import com.travel.dto.user.request.AdminUserPreferenceListRequest;
import com.travel.dto.user.request.AdminUserViewListRequest;
import com.travel.service.AdminUserInsightService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 管理端用户运营洞察控制器。
 */
@Tag(name = "管理端-用户运营", description = "用户偏好、收藏、浏览行为相关接口")
@RestController
@RequestMapping("/api/admin/v1/user-insights")
@RequiredArgsConstructor
public class AdminUserInsightController {

    private final AdminUserInsightService adminUserInsightService;

    @Operation(summary = "获取用户偏好列表")
    @GetMapping("/preferences")
    public ApiResponse<PageResult<AdminUserPreferenceListItem>> getPreferences(AdminUserPreferenceListRequest request) {
        return ApiResponse.success(adminUserInsightService.getPreferenceList(request));
    }

    @Operation(summary = "获取用户收藏列表")
    @GetMapping("/favorites")
    public ApiResponse<PageResult<AdminUserFavoriteListItem>> getFavorites(AdminUserFavoriteListRequest request) {
        return ApiResponse.success(adminUserInsightService.getFavoriteList(request));
    }

    @Operation(summary = "删除收藏记录")
    @DeleteMapping("/favorites/{id}")
    public ApiResponse<Void> deleteFavorite(@PathVariable("id") Long id) {
        adminUserInsightService.deleteFavorite(id);
        return ApiResponse.success();
    }

    @Operation(summary = "获取浏览行为列表")
    @GetMapping("/views")
    public ApiResponse<PageResult<AdminUserViewListItem>> getViews(AdminUserViewListRequest request) {
        return ApiResponse.success(adminUserInsightService.getViewList(request));
    }

    @Operation(summary = "删除浏览行为记录")
    @DeleteMapping("/views/{id}")
    public ApiResponse<Void> deleteView(@PathVariable("id") Long id) {
        adminUserInsightService.deleteView(id);
        return ApiResponse.success();
    }
}

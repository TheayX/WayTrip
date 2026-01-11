package com.travel.controller;

import com.travel.common.result.ApiResponse;
import com.travel.common.result.PageResult;
import com.travel.dto.favorite.FavoriteRequest;
import com.travel.dto.spot.SpotListResponse;
import com.travel.service.FavoriteService;
import com.travel.util.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.Map;

/**
 * 收藏接口
 */
@Tag(name = "收藏")
@RestController
@RequestMapping("/api/v1/favorites")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;

    @Operation(summary = "添加收藏")
    @PostMapping
    public ApiResponse<Void> addFavorite(@Valid @RequestBody FavoriteRequest request) {
        Long userId = UserContext.getUserId();
        favoriteService.addFavorite(userId, request.getSpotId());
        return ApiResponse.success();
    }

    @Operation(summary = "取消收藏")
    @DeleteMapping("/{spotId}")
    public ApiResponse<Void> removeFavorite(@PathVariable Long spotId) {
        Long userId = UserContext.getUserId();
        favoriteService.removeFavorite(userId, spotId);
        return ApiResponse.success();
    }

    @Operation(summary = "检查收藏状态")
    @GetMapping("/check/{spotId}")
    public ApiResponse<Map<String, Boolean>> checkFavorite(@PathVariable Long spotId) {
        Long userId = UserContext.getUserId();
        boolean isFavorite = favoriteService.isFavorite(userId, spotId);
        return ApiResponse.success(Map.of("isFavorite", isFavorite));
    }

    @Operation(summary = "获取收藏列表")
    @GetMapping
    public ApiResponse<PageResult<SpotListResponse>> getFavoriteList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Long userId = UserContext.getUserId();
        return ApiResponse.success(favoriteService.getFavoriteList(userId, page, pageSize));
    }
}

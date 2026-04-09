package com.travel.controller.app;

import com.travel.common.result.ApiResponse;
import com.travel.common.result.PageResult;
import com.travel.dto.favorite.request.FavoriteRequest;
import com.travel.dto.spot.response.SpotListResponse;
import com.travel.service.FavoriteService;
import com.travel.util.web.UserContextHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.Map;

/**
 * 用户端收藏控制器，负责景点收藏相关接口。
 * <p>
 * 收藏能力全部绑定当前登录用户执行，控制器层不接受外部传入用户标识，避免越权操作。
 */
@Tag(name = "用户端-收藏", description = "用户收藏管理相关接口")
@RestController
@RequestMapping("/api/v1/favorites")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;

    @Operation(summary = "添加收藏")
    @PostMapping
    public ApiResponse<Void> addFavorite(@Valid @RequestBody FavoriteRequest request) {
        // 用户身份统一从上下文读取，防止前端伪造 userId 操作他人收藏。
        Long userId = UserContextHolder.getUserId();
        favoriteService.addFavorite(userId, request.getSpotId());
        return ApiResponse.success();
    }

    @Operation(summary = "取消收藏")
    @DeleteMapping("/{spotId}")
    public ApiResponse<Void> removeFavorite(@PathVariable("spotId") Long spotId) {
        Long userId = UserContextHolder.getUserId();
        favoriteService.removeFavorite(userId, spotId);
        return ApiResponse.success();
    }

    @Operation(summary = "检查收藏状态")
    @GetMapping("/check/{spotId}")
    public ApiResponse<Map<String, Boolean>> checkFavorite(@PathVariable("spotId") Long spotId) {
        Long userId = UserContextHolder.getUserId();
        boolean isFavorite = favoriteService.isFavorite(userId, spotId);
        return ApiResponse.success(Map.of("isFavorite", isFavorite));
    }

    @Operation(summary = "获取收藏列表")
    @GetMapping
    public ApiResponse<PageResult<SpotListResponse>> getFavoriteList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Long userId = UserContextHolder.getUserId();
        return ApiResponse.success(favoriteService.getFavoriteList(userId, page, pageSize));
    }
}

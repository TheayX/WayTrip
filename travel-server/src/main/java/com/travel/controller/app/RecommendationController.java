package com.travel.controller.app;

import com.travel.common.result.ApiResponse;
import com.travel.dto.recommendation.response.RecommendationResponse;
import com.travel.dto.recommendation.response.SimilarityPreviewResponse;
import com.travel.service.RecommendationService;
import com.travel.util.web.UserContextHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 用户端推荐控制器，负责推荐结果获取与刷新接口。
 * <p>
 * 推荐结果读取与强制刷新分开暴露，便于前端根据场景选择走缓存还是实时重算。
 */
@Tag(name = "用户端-推荐", description = "个性化推荐相关接口")
@RestController
@RequestMapping("/api/v1/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;

    @Operation(summary = "获取个性化推荐")
    @GetMapping
    public ApiResponse<RecommendationResponse> getRecommendations(
            @RequestParam(defaultValue = "10") Integer limit) {
        // 推荐结果永远基于当前用户上下文生成，不接受外部指定 userId。
        Long userId = UserContextHolder.getUserId();
        return ApiResponse.success(recommendationService.getRecommendations(userId, limit));
    }

    @Operation(summary = "刷新推荐")
    @PostMapping("/refresh")
    public ApiResponse<RecommendationResponse> refreshRecommendations(
            @RequestParam(defaultValue = "10") Integer limit) {
        Long userId = UserContextHolder.getUserId();
        return ApiResponse.success(recommendationService.refreshRecommendations(userId, limit));
    }

    @Operation(summary = "获取相似景点推荐")
    @GetMapping("/similar")
    public ApiResponse<SimilarityPreviewResponse> getSimilarSpots(
            @RequestParam Long spotId,
            @RequestParam(defaultValue = "6") Integer limit) {
        return ApiResponse.success(recommendationService.previewSimilarityNeighbors(spotId, limit));
    }
}

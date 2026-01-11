package com.travel.controller;

import com.travel.common.result.ApiResponse;
import com.travel.dto.recommendation.RecommendationResponse;
import com.travel.service.RecommendationService;
import com.travel.util.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "推荐接口", description = "个性化推荐相关接口")
@RestController
@RequestMapping("/api/v1/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;

    @Operation(summary = "获取个性化推荐")
    @GetMapping
    public ApiResponse<RecommendationResponse> getRecommendations(
            @RequestParam(defaultValue = "10") Integer limit) {
        Long userId = UserContext.getUserId();
        return ApiResponse.success(recommendationService.getRecommendations(userId, limit));
    }

    @Operation(summary = "刷新推荐")
    @PostMapping("/refresh")
    public ApiResponse<RecommendationResponse> refreshRecommendations(
            @RequestParam(defaultValue = "10") Integer limit) {
        Long userId = UserContext.getUserId();
        return ApiResponse.success(recommendationService.refreshRecommendations(userId, limit));
    }
}

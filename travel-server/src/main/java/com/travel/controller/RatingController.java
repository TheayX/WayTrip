package com.travel.controller;

import com.travel.common.result.ApiResponse;
import com.travel.common.result.PageResult;
import com.travel.dto.rating.RatingRequest;
import com.travel.dto.rating.RatingResponse;
import com.travel.service.RatingService;
import com.travel.util.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

/**
 * 评分接口
 */
@Tag(name = "评分")
@RestController
@RequestMapping("/api/v1/ratings")
@RequiredArgsConstructor
public class RatingController {

    private final RatingService ratingService;

    @Operation(summary = "提交评分")
    @PostMapping
    public ApiResponse<Void> submitRating(@Valid @RequestBody RatingRequest request) {
        Long userId = UserContext.getUserId();
        ratingService.submitRating(userId, request);
        return ApiResponse.success();
    }

    @Operation(summary = "获取用户对景点的评分")
    @GetMapping("/spot/{spotId}")
    public ApiResponse<RatingResponse> getUserRating(@PathVariable("spotId") Long spotId) {
        Long userId = UserContext.getUserId();
        return ApiResponse.success(ratingService.getUserRating(userId, spotId));
    }

    @Operation(summary = "获取景点评论列表")
    @GetMapping("/spot/{spotId}/comments")
    public ApiResponse<PageResult<RatingResponse>> getSpotRatings(
            @PathVariable("spotId") Long spotId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return ApiResponse.success(ratingService.getSpotRatings(spotId, page, pageSize));
    }
}

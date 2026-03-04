package com.travel.controller;

import com.travel.common.result.ApiResponse;
import com.travel.common.result.PageResult;
import com.travel.dto.rating.RatingRequest;
import com.travel.dto.rating.RatingResponse;
import com.travel.service.ReviewService;
import com.travel.util.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

/**
 * 评价接口
 */
@Tag(name = "评价")
@RestController
@RequestMapping("/api/v1/ratings")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @Operation(summary = "提交评价")
    @PostMapping
    public ApiResponse<Void> submitRating(@Valid @RequestBody RatingRequest request) {
        Long userId = UserContext.getUserId();
        reviewService.submitRating(userId, request);
        return ApiResponse.success();
    }

    @Operation(summary = "获取用户对景点的评价")
    @GetMapping("/spot/{spotId}")
    public ApiResponse<RatingResponse> getUserRating(@PathVariable("spotId") Long spotId) {
        Long userId = UserContext.getUserId();
        return ApiResponse.success(reviewService.getUserRating(userId, spotId));
    }

    @Operation(summary = "获取景点评论列表")
    @GetMapping("/spot/{spotId}/comments")
    public ApiResponse<PageResult<RatingResponse>> getSpotRatings(
            @PathVariable("spotId") Long spotId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return ApiResponse.success(reviewService.getSpotRatings(spotId, page, pageSize));
    }
}


package com.travel.controller.app;

import com.travel.common.result.ApiResponse;
import com.travel.common.result.PageResult;
import com.travel.dto.review.ReviewRequest;
import com.travel.dto.review.ReviewResponse;
import com.travel.service.ReviewService;
import com.travel.util.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

/**
 * 用户端评价接口
 */
@Tag(name = "用户端-评价", description = "用户评价提交与查看相关接口")
@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @Operation(summary = "提交评价")
    @PostMapping
    public ApiResponse<Void> submitReview(@Valid @RequestBody ReviewRequest request) {
        Long userId = UserContext.getUserId();
        reviewService.submitReview(userId, request);
        return ApiResponse.success();
    }

    @Operation(summary = "获取用户对景点的评价")
    @GetMapping("/spot/{spotId}")
    public ApiResponse<ReviewResponse> getUserReview(@PathVariable("spotId") Long spotId) {
        Long userId = UserContext.getUserId();
        return ApiResponse.success(reviewService.getUserReview(userId, spotId));
    }

    @Operation(summary = "获取景点评论列表")
    @GetMapping("/spot/{spotId}/comments")
    public ApiResponse<PageResult<ReviewResponse>> getSpotReviews(
            @PathVariable("spotId") Long spotId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return ApiResponse.success(reviewService.getSpotReviews(spotId, page, pageSize));
    }
}


package com.travel.controller.app;

import com.travel.common.result.ApiResponse;
import com.travel.common.result.PageResult;
import com.travel.dto.review.request.ReviewFeedRequest;
import com.travel.dto.review.request.ReviewRequest;
import com.travel.dto.review.response.ReviewResponse;
import com.travel.service.ReviewService;
import com.travel.util.web.UserContextHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户端评价控制器，负责评价提交、查询与删除接口。
 * <p>
 * 评价写操作需要绑定当前用户，公开查询则保持无状态，便于详情页和口碑流复用。
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
        // 用户身份不从请求体透传，避免出现替他人写评价的越权风险。
        Long userId = UserContextHolder.getUserId();
        reviewService.submitReview(userId, request);
        return ApiResponse.success();
    }

    @Operation(summary = "获取用户对景点的评价")
    @GetMapping("/spot/{spotId}")
    public ApiResponse<ReviewResponse> getUserReview(@PathVariable("spotId") Long spotId) {
        Long userId = UserContextHolder.getUserId();
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

    @Operation(summary = "获取口碑流")
    @GetMapping("/feed")
    public ApiResponse<PageResult<ReviewResponse>> getReviewFeed(ReviewFeedRequest request) {
        return ApiResponse.success(reviewService.getReviewFeed(request));
    }

    @Operation(summary = "获取当前用户的评价列表")
    @GetMapping("/mine")
    public ApiResponse<PageResult<ReviewResponse>> getMyReviews(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Long userId = UserContextHolder.getUserId();
        return ApiResponse.success(reviewService.getUserReviews(userId, page, pageSize));
    }

    @Operation(summary = "删除自己的评价")
    @DeleteMapping("/{reviewId}")
    public ApiResponse<Void> deleteReview(@PathVariable("reviewId") Long reviewId) {
        Long userId = UserContextHolder.getUserId();
        reviewService.deleteReview(userId, reviewId);
        return ApiResponse.success();
    }
}

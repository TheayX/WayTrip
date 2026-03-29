package com.travel.controller.admin;

import com.travel.common.result.ApiResponse;
import com.travel.common.result.PageResult;
import com.travel.dto.review.AdminReviewListRequest;
import com.travel.dto.review.ReviewResponse;
import com.travel.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端评价控制器，负责评价检索接口。
 */
@Tag(name = "管理端-评价", description = "管理端评价查询相关接口")
@RestController
@RequestMapping("/api/admin/v1/reviews")
@RequiredArgsConstructor
public class AdminReviewController {

    private final ReviewService reviewService;

    @Operation(summary = "分页查询评价列表")
    @GetMapping
    public ApiResponse<PageResult<ReviewResponse>> getReviews(AdminReviewListRequest request) {
        return ApiResponse.success(reviewService.getAdminReviews(request));
    }
}

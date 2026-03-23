package com.travel.service;

import com.travel.common.result.PageResult;
import com.travel.dto.review.AdminReviewListRequest;
import com.travel.dto.review.ReviewRequest;
import com.travel.dto.review.ReviewResponse;

public interface ReviewService {

    void submitReview(Long userId, ReviewRequest request);

    ReviewResponse getUserReview(Long userId, Long spotId);

    PageResult<ReviewResponse> getSpotReviews(Long spotId, Integer page, Integer pageSize);

    PageResult<ReviewResponse> getUserReviews(Long userId, Integer page, Integer pageSize);

    void deleteReview(Long userId, Long reviewId);

    PageResult<ReviewResponse> getAdminReviews(AdminReviewListRequest request);

    int getUserReviewCount(Long userId);
}

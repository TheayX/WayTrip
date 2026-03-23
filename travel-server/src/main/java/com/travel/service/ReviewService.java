package com.travel.service;

import com.travel.common.result.PageResult;
import com.travel.dto.review.ReviewRequest;
import com.travel.dto.review.ReviewResponse;

/**
 * 评价服务接口
 */
public interface ReviewService {

    /**
     * 提交评价
     */
    void submitReview(Long userId, ReviewRequest request);

    /**
     * 获取用户对景点的评价
     */
    ReviewResponse getUserReview(Long userId, Long spotId);

    /**
     * 获取景点评论列表
     */
    PageResult<ReviewResponse> getSpotReviews(Long spotId, Integer page, Integer pageSize);

    /**
     * 删除自己的评价
     */
    void deleteReview(Long userId, Long reviewId);

    /**
     * 获取用户评价数量
     */
    int getUserReviewCount(Long userId);
}


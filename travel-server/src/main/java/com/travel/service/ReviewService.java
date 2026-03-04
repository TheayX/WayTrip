package com.travel.service;

import com.travel.common.result.PageResult;
import com.travel.dto.rating.RatingRequest;
import com.travel.dto.rating.RatingResponse;

/**
 * 评价服务接口
 */
public interface ReviewService {

    /**
     * 提交评价
     */
    void submitRating(Long userId, RatingRequest request);

    /**
     * 获取用户对景点的评价
     */
    RatingResponse getUserRating(Long userId, Long spotId);

    /**
     * 获取景点评论列表
     */
    PageResult<RatingResponse> getSpotRatings(Long spotId, Integer page, Integer pageSize);

    /**
     * 获取用户评价数量
     */
    int getUserRatingCount(Long userId);
}


package com.travel.service;

import com.travel.common.result.PageResult;
import com.travel.dto.rating.RatingRequest;
import com.travel.dto.rating.RatingResponse;

/**
 * 评分服务接口
 */
public interface RatingService {
    
    /**
     * 提交评分
     */
    void submitRating(Long userId, RatingRequest request);
    
    /**
     * 获取用户对景点的评分
     */
    RatingResponse getUserRating(Long userId, Long spotId);
    
    /**
     * 获取景点评论列表
     */
    PageResult<RatingResponse> getSpotRatings(Long spotId, Integer page, Integer pageSize);
    
    /**
     * 获取用户评分数量
     */
    int getUserRatingCount(Long userId);
}

package com.travel.service;

import com.travel.dto.home.HotSpotResponse;
import com.travel.dto.recommendation.RecommendationResponse;

/**
 * 推荐服务接口
 */
public interface RecommendationService {

    /**
     * 获取个性化推荐
     */
    RecommendationResponse getRecommendations(Long userId, Integer limit);

    /**
     * 刷新推荐（重新计算）
     */
    RecommendationResponse refreshRecommendations(Long userId, Integer limit);

    /**
     * 获取热门景点
     */
    HotSpotResponse getHotSpots(Integer limit);

    /**
     * 计算并更新物品相似度矩阵
     */
    void updateSimilarityMatrix();
}

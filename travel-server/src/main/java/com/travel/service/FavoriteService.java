package com.travel.service;

import com.travel.common.result.PageResult;
import com.travel.dto.spot.SpotListResponse;

/**
 * 收藏服务接口
 */
public interface FavoriteService {
    
    /**
     * 添加收藏
     */
    void addFavorite(Long userId, Long spotId);
    
    /**
     * 取消收藏
     */
    void removeFavorite(Long userId, Long spotId);
    
    /**
     * 检查是否已收藏
     */
    boolean isFavorite(Long userId, Long spotId);
    
    /**
     * 获取收藏列表
     */
    PageResult<SpotListResponse> getFavoriteList(Long userId, Integer page, Integer pageSize);
}

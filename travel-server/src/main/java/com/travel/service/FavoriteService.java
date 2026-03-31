package com.travel.service;

import com.travel.common.result.PageResult;
import com.travel.dto.spot.response.SpotListResponse;

/**
 * 收藏服务接口。
 * <p>
 * 定义收藏新增、取消、状态查询和收藏列表能力。
 */
public interface FavoriteService {
    
    /**
     * 添加景点收藏。
     *
     * @param userId 当前登录用户 ID
     * @param spotId 景点 ID
     */
    void addFavorite(Long userId, Long spotId);
    
    /**
     * 取消景点收藏。
     *
     * @param userId 当前登录用户 ID
     * @param spotId 景点 ID
     */
    void removeFavorite(Long userId, Long spotId);
    
    /**
     * 检查当前用户是否已收藏指定景点。
     *
     * @param userId 当前登录用户 ID
     * @param spotId 景点 ID
     * @return 是否已收藏
     */
    boolean isFavorite(Long userId, Long spotId);
    
    /**
     * 分页获取当前用户收藏列表。
     *
     * @param userId 当前登录用户 ID
     * @param page 当前页码
     * @param pageSize 每页条数
     * @return 收藏分页结果
     */
    PageResult<SpotListResponse> getFavoriteList(Long userId, Integer page, Integer pageSize);
}

package com.travel.service;

import com.travel.common.result.PageResult;
import com.travel.dto.user.model.AdminUserFavoriteListItem;
import com.travel.dto.user.model.AdminUserPreferenceListItem;
import com.travel.dto.user.model.AdminUserViewListItem;
import com.travel.dto.user.request.AdminUserFavoriteListRequest;
import com.travel.dto.user.request.AdminUserPreferenceListRequest;
import com.travel.dto.user.request.AdminUserViewListRequest;

/**
 * 管理端用户运营洞察服务。
 * <p>
 * 提供用户偏好、收藏行为、浏览行为的查询与治理能力。
 */
public interface AdminUserInsightService {

    /**
     * 分页获取用户偏好列表。
     *
     * @param request 查询条件
     * @return 用户偏好分页结果
     */
    PageResult<AdminUserPreferenceListItem> getPreferenceList(AdminUserPreferenceListRequest request);

    /**
     * 分页获取用户收藏列表。
     *
     * @param request 查询条件
     * @return 用户收藏分页结果
     */
    PageResult<AdminUserFavoriteListItem> getFavoriteList(AdminUserFavoriteListRequest request);

    /**
     * 删除指定收藏记录。
     *
     * @param favoriteId 收藏记录 ID
     */
    void deleteFavorite(Long favoriteId);

    /**
     * 分页获取用户浏览记录列表。
     *
     * @param request 查询条件
     * @return 用户浏览分页结果
     */
    PageResult<AdminUserViewListItem> getViewList(AdminUserViewListRequest request);

    /**
     * 删除指定浏览记录。
     *
     * @param viewId 浏览记录 ID
     */
    void deleteView(Long viewId);
}

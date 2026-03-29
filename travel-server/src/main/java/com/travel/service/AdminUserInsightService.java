package com.travel.service;

import com.travel.common.result.PageResult;
import com.travel.dto.user.*;

/**
 * 管理端用户运营洞察服务。
 */
public interface AdminUserInsightService {

    PageResult<AdminUserPreferenceListItem> getPreferenceList(AdminUserPreferenceListRequest request);

    PageResult<AdminUserFavoriteListItem> getFavoriteList(AdminUserFavoriteListRequest request);

    void deleteFavorite(Long favoriteId);

    PageResult<AdminUserViewListItem> getViewList(AdminUserViewListRequest request);

    void deleteView(Long viewId);
}

package com.travel.service;

import com.travel.dto.user.*;

/**
 * 用户服务接口
 */
public interface UserService {

    /**
     * 获取用户列表（管理端）
     */
    AdminUserListResponse getAdminUsers(AdminUserListRequest request);

    /**
     * 获取用户详情（管理端）
     */
    AdminUserDetailResponse getAdminUserDetail(Long userId);
}

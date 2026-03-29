package com.travel.service;

import com.travel.dto.user.*;

/**
 * 用户管理服务接口。
 * <p>
 * 定义管理端用户列表、详情和密码重置能力。
 */
public interface UserService {

    /**
     * 分页获取管理端用户列表。
     *
     * @param request 用户查询参数
     * @return 用户分页结果
     */
    AdminUserListResponse getAdminUsers(AdminUserListRequest request);

    /**
     * 获取管理端用户详情。
     *
     * @param userId 用户 ID
     * @return 用户详情
     */
    AdminUserDetailResponse getAdminUserDetail(Long userId);

    /**
     * 重置指定用户密码。
     *
     * @param userId 用户 ID
     * @param request 重置密码参数
     */
    void resetUserPassword(Long userId, ResetUserPasswordRequest request);
}

package com.travel.service;

import com.travel.dto.auth.request.ChangePasswordRequest;
import com.travel.dto.auth.request.UpdateUserInfoRequest;
import com.travel.dto.user.response.UserInfoResponse;

import java.util.List;

/**
 * 用户账户服务接口。
 * <p>
 * 定义用户资料查询、资料修改、密码修改、注销和偏好设置能力。
 */
public interface UserAccountService {

    /**
     * 获取当前用户资料。
     *
     * @param userId 当前登录用户 ID
     * @return 用户资料
     */
    UserInfoResponse getUserInfo(Long userId);

    /**
     * 更新当前用户资料。
     *
     * @param userId 当前登录用户 ID
     * @param request 用户资料更新参数
     */
    void updateUserInfo(Long userId, UpdateUserInfoRequest request);

    /**
     * 修改当前用户密码。
     *
     * @param userId 当前登录用户 ID
     * @param request 密码修改参数
     */
    void changePassword(Long userId, ChangePasswordRequest request);

    /**
     * 注销当前用户账户。
     *
     * @param userId 当前登录用户 ID
     */
    void deactivateAccount(Long userId);

    /**
     * 管理员封禁指定用户。
     *
     * @param userId 目标用户 ID
     */
    void deactivateAccountByAdmin(Long userId);

    /**
     * 管理员解封指定用户。
     *
     * @param userId 目标用户 ID
     */
    void reactivateAccountByAdmin(Long userId);

    /**
     * 设置当前用户偏好分类。
     *
     * @param userId 当前登录用户 ID
     * @param categoryIds 偏好分类 ID 列表
     */
    void setPreferences(Long userId, List<Long> categoryIds);
}

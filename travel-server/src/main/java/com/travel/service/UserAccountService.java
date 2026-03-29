package com.travel.service;

import com.travel.dto.auth.ChangePasswordRequest;
import com.travel.dto.auth.UpdateUserInfoRequest;
import com.travel.dto.auth.UserInfoResponse;

import java.util.List;

/**
 * 用户账户服务接口
 */
public interface UserAccountService {

    /**
     * 获取用户信息
     */
    UserInfoResponse getUserInfo(Long userId);

    /**
     * 更新用户信息
     */
    void updateUserInfo(Long userId, UpdateUserInfoRequest request);

    /**
     * 修改密码
     */
    void changePassword(Long userId, ChangePasswordRequest request);

    /**
     * 注销账户
     */
    void deactivateAccount(Long userId);

    /**
     * 设置用户偏好标签
     */
    void setPreferences(Long userId, List<Long> categoryIds);
}

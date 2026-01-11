package com.travel.service;

import com.travel.dto.auth.*;
import java.util.List;

/**
 * 认证服务接口
 */
public interface AuthService {
    
    /**
     * 微信登录
     */
    LoginResponse wxLogin(String code);
    
    /**
     * 获取用户信息
     */
    UserInfoResponse getUserInfo(Long userId);
    
    /**
     * 更新用户信息
     */
    void updateUserInfo(Long userId, UpdateUserInfoRequest request);
    
    /**
     * 设置用户偏好标签
     */
    void setPreferences(Long userId, List<String> tags);
    
    /**
     * 管理员登录
     */
    AdminLoginResponse adminLogin(AdminLoginRequest request);
    
    /**
     * 获取管理员信息
     */
    AdminLoginResponse.AdminInfo getAdminInfo(Long adminId);
}

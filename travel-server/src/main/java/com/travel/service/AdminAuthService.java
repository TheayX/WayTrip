package com.travel.service;

import com.travel.dto.auth.AdminLoginRequest;
import com.travel.dto.auth.AdminLoginResponse;

/**
 * 管理员认证服务接口
 */
public interface AdminAuthService {

    /**
     * 管理员登录
     */
    AdminLoginResponse adminLogin(AdminLoginRequest request);

    /**
     * 获取管理员信息
     */
    AdminLoginResponse.AdminInfo getAdminInfo(Long adminId);
}

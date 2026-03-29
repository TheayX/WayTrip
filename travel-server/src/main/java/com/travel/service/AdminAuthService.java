package com.travel.service;

import com.travel.dto.auth.AdminLoginRequest;
import com.travel.dto.auth.AdminLoginResponse;

/**
 * 管理员认证服务接口。
 * <p>
 * 定义管理员登录和管理员信息查询能力。
 */
public interface AdminAuthService {

    /**
     * 执行管理员登录。
     *
     * @param request 管理员登录参数
     * @return 管理员登录结果
     */
    AdminLoginResponse adminLogin(AdminLoginRequest request);

    /**
     * 获取管理员基础信息。
     *
     * @param adminId 管理员 ID
     * @return 管理员基础信息
     */
    AdminLoginResponse.AdminInfo getAdminInfo(Long adminId);
}

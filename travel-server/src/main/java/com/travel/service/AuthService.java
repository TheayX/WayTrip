package com.travel.service;

import com.travel.dto.auth.*;
import java.util.List;

/**
 * 认证服务接口
 */
public interface AuthService {
    
    /**
     * 微信登录（老用户返回token，新用户只返回openid）
     */
    WxLoginResponse wxLogin(String code);

    /**
     * Web端注册（手机号+密码）
     */
    LoginResponse webRegister(WebRegisterRequest request);

    /**
     * Web端登录（手机号+密码）
     */
    LoginResponse webLogin(WebLoginRequest request);

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

    /**
     * 小程序端绑定手机号（新用户注册或匹配已有账户合并openid）
     */
    LoginResponse wxBindPhone(WxBindPhoneRequest request);

    /**
     * 小程序端第一步校验手机号+密码；已有账户则直接完成绑定登录，
     * 新用户则仅校验通过，不立即创建账户。
     * 返回 null 表示需要进入第二步继续完成注册。
     */
    LoginResponse prepareWxBindPhone(WxBindPhoneRequest request);

    /**
     * 管理员登录
     */
    AdminLoginResponse adminLogin(AdminLoginRequest request);
    
    /**
     * 获取管理员信息
     */
    AdminLoginResponse.AdminInfo getAdminInfo(Long adminId);
}

package com.travel.service;

import com.travel.dto.auth.LoginResponse;
import com.travel.dto.auth.WebLoginRequest;
import com.travel.dto.auth.WebRegisterRequest;
import com.travel.dto.auth.WxBindPhoneRequest;
import com.travel.dto.auth.WxLoginResponse;

/**
 * 用户认证服务接口
 */
public interface UserAuthService {

    /**
     * 微信登录（老用户返回token，新用户只返回openid）
     */
    WxLoginResponse wxLogin(String code);

    /**
     * Web端注册（手机号+密码）
     */
    LoginResponse webRegister(WebRegisterRequest request);

    /**
     * Web端第一步校验手机号是否可注册。
     */
    void prepareWebRegister(WebRegisterRequest request);

    /**
     * Web端登录（手机号+密码）
     */
    LoginResponse webLogin(WebLoginRequest request);

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
}

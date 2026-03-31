package com.travel.service;

import com.travel.dto.auth.response.LoginResponse;
import com.travel.dto.auth.request.WebLoginRequest;
import com.travel.dto.auth.request.WebRegisterRequest;
import com.travel.dto.auth.request.WxBindPhoneRequest;
import com.travel.dto.auth.response.WxLoginResponse;

/**
 * 用户认证服务接口。
 * <p>
 * 定义微信登录、Web 注册登录以及手机号绑定相关能力。
 */
public interface UserAuthService {

    /**
     * 执行微信登录。
     *
     * @param code 微信登录临时凭证
     * @return 微信登录结果；老用户返回 Token，新用户仅返回 OpenID
     */
    WxLoginResponse wxLogin(String code);

    /**
     * 执行 Web 端注册。
     *
     * @param request Web 注册参数
     * @return 登录结果
     */
    LoginResponse webRegister(WebRegisterRequest request);

    /**
     * 预校验 Web 端手机号是否允许注册。
     *
     * @param request Web 注册参数
     */
    void prepareWebRegister(WebRegisterRequest request);

    /**
     * 执行 Web 端登录。
     *
     * @param request Web 登录参数
     * @return 登录结果
     */
    LoginResponse webLogin(WebLoginRequest request);

    /**
     * 小程序端绑定手机号。
     *
     * @param request 绑定手机号参数
     * @return 登录结果
     */
    LoginResponse wxBindPhone(WxBindPhoneRequest request);

    /**
     * 小程序端预校验手机号和密码。
     * <p>
     * 已有账户时直接完成绑定登录；新用户仅校验通过，不立即创建账户。
     *
     * @param request 绑定手机号参数
     * @return 已可直接登录时返回登录结果；需要继续完成注册时返回 {@code null}
     */
    LoginResponse prepareWxBindPhone(WxBindPhoneRequest request);
}

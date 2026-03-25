package com.travel.dto.auth;

import lombok.Builder;
import lombok.Data;

/**
 * 小程序端第一步校验手机号+密码响应
 */
@Data
@Builder
public class WxPrepareBindPhoneResponse {

    /**
     * 是否已经直接完成登录（匹配并合并了已有账户）
     */
    private Boolean completed;

    /**
     * 是否需要进入第二步补充资料后再正式创建账户
     */
    private Boolean requireProfile;

    /**
     * 已完成时返回登录信息
     */
    private LoginResponse login;
}

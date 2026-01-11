package com.travel.common.result;

import lombok.Getter;

/**
 * 错误码枚举
 */
@Getter
public enum ResultCode {

    // 成功
    SUCCESS(0, "success"),

    // 认证相关 10xxx
    WX_LOGIN_FAILED(10001, "微信登录失败"),
    TOKEN_INVALID(10002, "Token无效或过期"),
    ACCESS_DENIED(10003, "无权限访问"),
    ADMIN_LOGIN_FAILED(10004, "用户名或密码错误"),

    // 景点相关 20xxx
    SPOT_NOT_FOUND(20001, "景点不存在"),
    SPOT_OFFLINE(20002, "景点已下架"),

    // 攻略相关 30xxx
    GUIDE_NOT_FOUND(30001, "攻略不存在"),
    GUIDE_OFFLINE(30002, "攻略已下架"),

    // 订单相关 40xxx
    ORDER_NOT_FOUND(40001, "订单不存在"),
    ORDER_STATUS_ERROR(40002, "订单状态不允许此操作"),
    ORDER_ALREADY_PAID(40003, "订单已支付"),
    ORDER_ALREADY_CANCELLED(40004, "订单已取消"),

    // 评分相关 50xxx
    RATING_INVALID(50001, "评分值无效（需1-5）"),

    // 通用错误 60xxx
    PARAM_ERROR(60001, "参数校验失败"),
    SYSTEM_ERROR(60002, "服务器内部错误");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}

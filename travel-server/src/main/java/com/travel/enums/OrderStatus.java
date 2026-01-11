package com.travel.enums;

import lombok.Getter;

/**
 * 订单状态枚举
 */
@Getter
public enum OrderStatus {

    PENDING_PAYMENT("待支付"),
    PENDING_USE("待使用"),
    COMPLETED("已完成"),
    CANCELLED("已取消");

    private final String description;

    OrderStatus(String description) {
        this.description = description;
    }

    /**
     * 判断是否可以取消
     */
    public boolean canCancel() {
        return this == PENDING_PAYMENT || this == PENDING_USE;
    }

    /**
     * 判断是否可以支付
     */
    public boolean canPay() {
        return this == PENDING_PAYMENT;
    }

    /**
     * 判断是否可以完成
     */
    public boolean canComplete() {
        return this == PENDING_USE;
    }
}

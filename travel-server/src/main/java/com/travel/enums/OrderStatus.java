package com.travel.enums;

import lombok.Getter;

/**
 * 订单状态枚举 - 与数据库 order.status 字段完全对应
 * 0-待支付，1-已支付，2-已取消，3-已退款，4-已完成
 */
@Getter
public enum OrderStatus {

    PENDING(0, "pending", "待支付"),
    PAID(1, "paid", "已支付"),
    CANCELLED(2, "cancelled", "已取消"),
    REFUNDED(3, "refunded", "已退款"),
    COMPLETED(4, "completed", "已完成");

    private final int code;
    private final String key;
    private final String description;

    OrderStatus(int code, String key, String description) {
        this.code = code;
        this.key = key;
        this.description = description;
    }

    /**
     * 根据数据库 code 获取枚举
     */
    public static OrderStatus fromCode(Integer code) {
        if (code == null) return null;
        for (OrderStatus status : values()) {
            if (status.code == code) return status;
        }
        return null;
    }

    /**
     * 根据前端传入的字符串 key 获取枚举
     */
    public static OrderStatus fromKey(String key) {
        if (key == null || key.isEmpty()) return null;
        for (OrderStatus status : values()) {
            if (status.key.equalsIgnoreCase(key)) return status;
        }
        return null;
    }

    /**
     * 判断是否可以支付
     */
    public boolean canPay() {
        return this == PENDING;
    }

    /**
     * 判断是否可以取消
     */
    public boolean canCancel() {
        return this == PENDING;
    }

    /**
     * 判断是否可以完成
     */
    public boolean canComplete() {
        return this == PAID;
    }

    /**
     * 判断是否可以退款
     */
    public boolean canRefund() {
        return this == PAID;
    }

    /**
     * 是否为有效订单（未取消）
     */
    public boolean isActive() {
        return this != CANCELLED;
    }

    /**
     * 是否产生了收入（已支付或已退款前的状态）
     */
    public boolean hasRevenue() {
        return this == PAID || this == REFUNDED;
    }
}

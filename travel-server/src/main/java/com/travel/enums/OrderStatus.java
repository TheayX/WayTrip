package com.travel.enums;

import lombok.Getter;

/**
 * 订单状态枚举，对应 order.status 字段取值。
 * <p>
 * 统一封装数据库状态码、前端状态标识和业务能力判断，避免状态流转规则散落在服务层。
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
     * 根据数据库状态码解析订单状态。
     */
    public static OrderStatus fromCode(Integer code) {
        if (code == null) return null;
        for (OrderStatus status : values()) {
            if (status.code == code) return status;
        }
        return null;
    }

    /**
     * 根据前端状态标识解析订单状态。
     */
    public static OrderStatus fromKey(String key) {
        if (key == null || key.isEmpty()) return null;
        for (OrderStatus status : values()) {
            if (status.key.equalsIgnoreCase(key)) return status;
        }
        return null;
    }

    /**
     * 判断当前状态是否允许支付。
     */
    public boolean canPay() {
        return this == PENDING;
    }

    /**
     * 判断当前状态是否允许取消。
     */
    public boolean canCancel() {
        return this == PENDING || this == PAID;
    }

    /**
     * 判断当前状态是否允许完成。
     */
    public boolean canComplete() {
        return this == PAID;
    }

    /**
     * 判断当前状态是否允许退款。
     */
    public boolean canRefund() {
        return this == PAID;
    }

    /**
     * 判断是否为未取消的有效订单。
     */
    public boolean isActive() {
        return this != CANCELLED;
    }

    /**
     * 判断当前状态是否计入收入口径。
     */
    public boolean hasRevenue() {
        return this == PAID || this == REFUNDED;
    }
}

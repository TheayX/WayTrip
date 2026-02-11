package com.travel.dto.order;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 订单详情响应
 */
@Data
public class OrderDetailResponse {

    private Long id;
    private String orderNo;
    private Long spotId;
    private String spotName;
    private String spotImage;
    private BigDecimal unitPrice;
    private Integer quantity;
    private BigDecimal totalPrice;
    private LocalDate visitDate;
    private String contactName;
    private String contactPhone;
    private String status;
    private String statusText;
    private LocalDateTime paidAt;
    private LocalDateTime cancelledAt;
    private LocalDateTime refundedAt;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;

    /**
     * 是否可支付
     */
    private Boolean canPay;

    /**
     * 是否可取消
     */
    private Boolean canCancel;
}

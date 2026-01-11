package com.travel.dto.order;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单列表响应
 */
@Data
public class OrderListResponse {

    private List<OrderItem> list;
    private Long total;
    private Integer page;
    private Integer pageSize;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItem {
        private Long id;
        private String orderNo;
        private Long spotId;
        private String spotName;
        private String spotImage;
        private BigDecimal unitPrice;
        private Integer quantity;
        private BigDecimal totalPrice;
        private LocalDate visitDate;
        private String status;
        private String statusText;
        private LocalDateTime createdAt;
    }
}

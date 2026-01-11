package com.travel.dto.order;

import lombok.Data;

/**
 * 订单列表请求（用户端）
 */
@Data
public class OrderListRequest {

    /**
     * 订单状态筛选：pending/paid/completed/cancelled
     */
    private String status;

    private Integer page = 1;

    private Integer pageSize = 10;
}

package com.travel.dto.order;

import lombok.Data;

/**
 * 用户端订单列表查询参数对象。
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

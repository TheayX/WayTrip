package com.travel.dto.order;

import lombok.Data;

import java.time.LocalDate;

/**
 * 管理端订单列表请求
 */
@Data
public class AdminOrderListRequest {

    /**
     * 订单号搜索
     */
    private String orderNo;

    /**
     * 景点名称搜索
     */
    private String spotName;

    /**
     * 订单状态筛选
     */
    private String status;

    /**
     * 开始日期
     */
    private LocalDate startDate;

    /**
     * 结束日期
     */
    private LocalDate endDate;

    private Integer page = 1;

    private Integer pageSize = 10;
}

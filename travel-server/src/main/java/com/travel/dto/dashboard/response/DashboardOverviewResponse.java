package com.travel.dto.dashboard.response;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 仪表盘概览响应对象。
 */
@Data
public class DashboardOverviewResponse {

    private Long totalUsers;
    private Long totalSpots;
    private Long totalOrders;
    private BigDecimal totalRevenue;

    private Long todayOrders;
    private BigDecimal todayRevenue;
    private Long todayNewUsers;
}

package com.travel.dto.dashboard;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 仪表板概览响应
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

package com.travel.dto.dashboard.response;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 仪表盘概览响应对象。
 * <p>
 * 聚合后台首页概览卡片、趋势和分布类统计所需的核心指标。
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
    private Long todayNewSpots;

    private Long yesterdayOrders;
    private BigDecimal yesterdayRevenue;
    private Long yesterdayNewUsers;
    private Long yesterdayNewSpots;

    private List<BigDecimal> recentRevenueSeries;
    private List<Long> recentUserSeries;
    private List<Long> recentSpotSeries;
    private List<Long> recentOrderSeries;
}

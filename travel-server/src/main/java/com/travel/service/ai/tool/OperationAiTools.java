package com.travel.service.ai.tool;

import com.travel.dto.dashboard.response.DashboardOverviewResponse;
import com.travel.dto.dashboard.response.HotSpotsResponse;
import com.travel.dto.dashboard.response.OrderTrendResponse;
import com.travel.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * AI 运营分析工具集合。
 */
@Component
@RequiredArgsConstructor
public class OperationAiTools {

    private final DashboardService dashboardService;
    private final AiToolContextHolder aiToolContextHolder;

    /**
     * 获取管理端运营概览。
     *
     * @return 运营概览摘要
     */
    @Tool(description = "获取管理端运营概览指标，包括用户、景点、订单、营收和最近趋势")
    public Map<String, Object> getOperationOverview() {
        aiToolContextHolder.requireCurrentAdminId();
        DashboardOverviewResponse overview = dashboardService.getOverview();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalUsers", overview.getTotalUsers());
        result.put("totalSpots", overview.getTotalSpots());
        result.put("totalOrders", overview.getTotalOrders());
        result.put("totalRevenue", overview.getTotalRevenue());
        result.put("todayOrders", overview.getTodayOrders());
        result.put("todayRevenue", overview.getTodayRevenue());
        result.put("todayNewUsers", overview.getTodayNewUsers());
        result.put("todayNewSpots", overview.getTodayNewSpots());
        result.put("yesterdayOrders", overview.getYesterdayOrders());
        result.put("yesterdayRevenue", overview.getYesterdayRevenue());
        result.put("yesterdayNewUsers", overview.getYesterdayNewUsers());
        result.put("yesterdayNewSpots", overview.getYesterdayNewSpots());
        result.put("recentRevenueSeries", overview.getRecentRevenueSeries());
        result.put("recentOrderSeries", overview.getRecentOrderSeries());
        result.put("recentUserSeries", overview.getRecentUserSeries());
        result.put("recentSpotSeries", overview.getRecentSpotSeries());
        aiToolContextHolder.addToolTrace(
                "getOperationOverview",
                "operation",
                true,
                "已获取管理端运营概览"
        );
        return AiToolResponse.success("已获取管理端运营概览", result);
    }

    /**
     * 获取订单趋势统计。
     *
     * @param days 统计天数
     * @param mode 统计口径
     * @return 订单趋势摘要
     */
    @Tool(description = "获取管理端订单趋势统计")
    public Map<String, Object> getOrderTrend(
            @ToolParam(description = "统计天数，建议 7 到 30", required = false) Integer days,
            @ToolParam(description = "统计口径：range 或 weekday", required = false) String mode) {
        aiToolContextHolder.requireCurrentAdminId();
        OrderTrendResponse trend = dashboardService.getOrderTrend(normalizeDays(days), normalizeMode(mode));
        aiToolContextHolder.addToolTrace(
                "getOrderTrend",
                "operation",
                true,
                "已获取订单趋势统计"
        );
        return AiToolResponse.success(
                "已获取订单趋势统计",
                Map.of("list", trend.getList())
        );
    }

    /**
     * 获取热门景点统计。
     *
     * @param limit 返回条数
     * @return 热门景点摘要
     */
    @Tool(description = "获取管理端热门景点统计")
    public Map<String, Object> getAdminHotSpots(
            @ToolParam(description = "返回条数，建议 3 到 10", required = false) Integer limit) {
        aiToolContextHolder.requireCurrentAdminId();
        HotSpotsResponse hotSpots = dashboardService.getHotSpots(normalizeLimit(limit));
        aiToolContextHolder.addToolTrace(
                "getAdminHotSpots",
                "operation",
                true,
                "已获取热门景点统计"
        );
        return AiToolResponse.success(
                "已获取热门景点统计",
                Map.of("list", hotSpots.getList())
        );
    }

    private Integer normalizeDays(Integer days) {
        if (days == null || days <= 0) {
            return 7;
        }
        return Math.min(days, 30);
    }

    private Integer normalizeLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return 5;
        }
        return Math.min(limit, 10);
    }

    private String normalizeMode(String mode) {
        return "range".equalsIgnoreCase(mode) ? "range" : "weekday";
    }
}

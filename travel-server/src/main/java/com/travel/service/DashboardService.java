package com.travel.service;

import com.travel.dto.dashboard.response.DashboardOverviewResponse;
import com.travel.dto.dashboard.response.HotSpotsResponse;
import com.travel.dto.dashboard.response.OrderHeatmapResponse;
import com.travel.dto.dashboard.response.OrderTrendResponse;

/**
 * 仪表盘服务接口。
 * <p>
 * 定义后台概览统计、订单趋势和热门景点统计能力。
 */
public interface DashboardService {

    /**
     * 获取后台概览统计数据。
     *
     * @return 仪表盘概览数据
     */
    DashboardOverviewResponse getOverview();

    /**
     * 获取订单趋势统计。
     *
     * @param days 统计天数
     * @param mode 统计口径：range-按日期连续统计，weekday-按星期聚合统计
     * @return 订单趋势数据
     */
    OrderTrendResponse getOrderTrend(Integer days, String mode);

    /**
     * 获取按天聚合的订单热力图统计。
     *
     * @param year 统计年份
     * @return 订单热力图数据
     */
    OrderHeatmapResponse getOrderHeatmap(Integer year);

    /**
     * 获取热门景点统计结果。
     *
     * @param limit 返回条数上限
     * @return 热门景点统计结果
     */
    HotSpotsResponse getHotSpots(Integer limit);
}

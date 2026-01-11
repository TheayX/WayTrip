package com.travel.service;

import com.travel.dto.dashboard.*;

/**
 * 仪表板服务接口
 */
public interface DashboardService {

    /**
     * 获取概览数据
     */
    DashboardOverviewResponse getOverview();

    /**
     * 获取订单趋势
     */
    OrderTrendResponse getOrderTrend(Integer days);

    /**
     * 获取热门景点
     */
    HotSpotsResponse getHotSpots(Integer limit);
}

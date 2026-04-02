package com.travel.service.impl;

import com.travel.dto.dashboard.response.OrderTrendResponse;
import com.travel.dto.dashboard.response.OrderHeatmapResponse;
import com.travel.entity.Order;
import com.travel.enums.OrderStatus;
import com.travel.mapper.OrderMapper;
import com.travel.mapper.SpotMapper;
import com.travel.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * 仪表盘趋势统计测试
 * 重点覆盖按星期聚合与按日期区间补齐两种展示口径。
 */
@ExtendWith(MockitoExtension.class)
class DashboardServiceImplTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private SpotMapper spotMapper;

    @Mock
    private OrderMapper orderMapper;

    private DashboardServiceImpl dashboardService;

    @BeforeEach
    void setUp() {
        dashboardService = new DashboardServiceImpl(userMapper, spotMapper, orderMapper);
    }

    @Test
    void getOrderTrend_shouldAggregateByWeekday_whenModeIsWeekday() {
        Order monday = buildOrder(1L, LocalDate.now().minusDays(45).with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY)).atTime(10, 0), "100.00");
        Order sunday = buildOrder(2L, LocalDate.now().minusDays(12).with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.SUNDAY)).atTime(11, 0), "200.00");

        when(orderMapper.selectList(any())).thenReturn(List.of(monday, sunday));

        OrderTrendResponse response = dashboardService.getOrderTrend(0, "weekday");

        assertEquals(7, response.getList().size());
        assertEquals("周一", response.getList().get(0).getDate());
        assertEquals(1L, response.getList().get(0).getOrderCount());
        assertEquals(new BigDecimal("100.00"), response.getList().get(0).getRevenue());
        assertEquals("周日", response.getList().get(6).getDate());
        assertEquals(1L, response.getList().get(6).getOrderCount());
        assertEquals(new BigDecimal("200.00"), response.getList().get(6).getRevenue());
    }

    @Test
    void getOrderTrend_shouldFillMissingDates_whenModeIsRange() {
        LocalDate today = LocalDate.now();
        Order todayOrder = buildOrder(1L, today.atTime(12, 0), "88.00");

        when(orderMapper.selectList(any())).thenReturn(List.of(todayOrder));

        OrderTrendResponse response = dashboardService.getOrderTrend(3, "range");

        assertEquals(3, response.getList().size());
        assertEquals(today.minusDays(2).toString(), response.getList().get(0).getDate());
        assertEquals(0L, response.getList().get(0).getOrderCount());
        assertEquals(BigDecimal.ZERO, response.getList().get(0).getRevenue());
        assertEquals(today.toString(), response.getList().get(2).getDate());
        assertEquals(1L, response.getList().get(2).getOrderCount());
        assertEquals(new BigDecimal("88.00"), response.getList().get(2).getRevenue());
    }

    @Test
    void getOrderTrend_shouldStartFromEarliestOrder_whenRangeUsesAllTime() {
        LocalDate today = LocalDate.now();
        Order oldestOrder = buildOrder(1L, today.minusDays(2).atTime(9, 0), "66.00");
        Order latestOrder = buildOrder(2L, today.atTime(14, 0), "99.00");

        when(orderMapper.selectList(any())).thenReturn(List.of(oldestOrder, latestOrder));

        OrderTrendResponse response = dashboardService.getOrderTrend(0, "range");

        assertEquals(3, response.getList().size());
        assertEquals(today.minusDays(2).toString(), response.getList().get(0).getDate());
        assertEquals(1L, response.getList().get(0).getOrderCount());
        assertEquals(today.toString(), response.getList().get(2).getDate());
        assertEquals(1L, response.getList().get(2).getOrderCount());
    }

    @Test
    void getOrderHeatmap_shouldFillWholeYear_whenSomeDatesHaveNoOrders() {
        Order firstOrder = buildOrder(1L, LocalDate.of(2026, 1, 2).atTime(10, 0), "50.00");
        Order secondOrder = buildOrder(2L, LocalDate.of(2026, 12, 31).atTime(15, 0), "70.00");

        when(orderMapper.selectList(any())).thenReturn(List.of(firstOrder, secondOrder));

        OrderHeatmapResponse response = dashboardService.getOrderHeatmap(2026);

        assertEquals(2026, response.getYear());
        assertEquals(365, response.getList().size());
        assertEquals("2026-01-01", response.getList().get(0).getDate());
        assertEquals(0L, response.getList().get(0).getOrderCount());
        assertEquals("2026-01-02", response.getList().get(1).getDate());
        assertEquals(1L, response.getList().get(1).getOrderCount());
        assertEquals("2026-12-31", response.getList().get(364).getDate());
        assertEquals(1L, response.getList().get(364).getOrderCount());
    }

    /**
     * 构造满足统计口径的有效订单夹具，便于聚焦趋势聚合逻辑。
     */
    private Order buildOrder(Long id, LocalDateTime createdAt, String amount) {
        Order order = new Order();
        order.setId(id);
        order.setCreatedAt(createdAt);
        order.setStatus(OrderStatus.PAID.getCode());
        order.setTotalAmount(new BigDecimal(amount));
        order.setIsDeleted(0);
        return order;
    }
}

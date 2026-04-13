package com.travel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.travel.common.constant.ResourceDisplayText;
import com.travel.dto.dashboard.response.DashboardOverviewResponse;
import com.travel.dto.dashboard.response.HotSpotsResponse;
import com.travel.dto.dashboard.response.OrderHeatmapResponse;
import com.travel.dto.dashboard.response.OrderTrendResponse;
import com.travel.entity.*;
import com.travel.enums.OrderStatus;
import com.travel.mapper.*;
import com.travel.service.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 仪表板服务实现，负责后台概览、趋势与热门景点统计。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    // 持久层依赖
    private final UserMapper userMapper;
    private final SpotMapper spotMapper;
    private final OrderMapper orderMapper;

    // 概览与趋势统计

    @Override
    public DashboardOverviewResponse getOverview() {
        DashboardOverviewResponse response = new DashboardOverviewResponse();

        // 总用户数
        response.setTotalUsers(userMapper.selectCount(
            new LambdaQueryWrapper<User>().eq(User::getIsDeleted, 0)
        ));

        // 总景点数
        response.setTotalSpots(spotMapper.selectCount(
            new LambdaQueryWrapper<Spot>()
                .eq(Spot::getIsPublished, 1)
                .eq(Spot::getIsDeleted, 0)
        ));

        // 已取消订单不计入总订单统计口径。
        List<Order> allOrders = orderMapper.selectList(
            new LambdaQueryWrapper<Order>()
                .eq(Order::getIsDeleted, 0)
                .ne(Order::getStatus, OrderStatus.CANCELLED.getCode())
        );
        response.setTotalOrders((long) allOrders.size());
        response.setTotalRevenue(sumRevenue(allOrders));

        // 今日统计统一按自然日零点切分。
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime yesterdayStart = LocalDate.now().minusDays(1).atStartOfDay();
        
        List<Order> todayOrders = orderMapper.selectList(
            new LambdaQueryWrapper<Order>()
                .eq(Order::getIsDeleted, 0)
                .ge(Order::getCreatedAt, todayStart)
                .ne(Order::getStatus, OrderStatus.CANCELLED.getCode())
        );
        response.setTodayOrders((long) todayOrders.size());
        response.setTodayRevenue(sumRevenue(todayOrders));

        List<Order> yesterdayOrders = orderMapper.selectList(
            new LambdaQueryWrapper<Order>()
                .eq(Order::getIsDeleted, 0)
                .ge(Order::getCreatedAt, yesterdayStart)
                .lt(Order::getCreatedAt, todayStart)
                .ne(Order::getStatus, OrderStatus.CANCELLED.getCode())
        );
        response.setYesterdayOrders((long) yesterdayOrders.size());
        response.setYesterdayRevenue(sumRevenue(yesterdayOrders));

        response.setTodayNewUsers(userMapper.selectCount(
            new LambdaQueryWrapper<User>()
                .eq(User::getIsDeleted, 0)
                .ge(User::getCreatedAt, todayStart)
        ));
        response.setYesterdayNewUsers(userMapper.selectCount(
            new LambdaQueryWrapper<User>()
                .eq(User::getIsDeleted, 0)
                .ge(User::getCreatedAt, yesterdayStart)
                .lt(User::getCreatedAt, todayStart)
        ));

        response.setTodayNewSpots(spotMapper.selectCount(
            new LambdaQueryWrapper<Spot>()
                .eq(Spot::getIsPublished, 1)
                .eq(Spot::getIsDeleted, 0)
                .ge(Spot::getCreatedAt, todayStart)
        ));
        response.setYesterdayNewSpots(spotMapper.selectCount(
            new LambdaQueryWrapper<Spot>()
                .eq(Spot::getIsPublished, 1)
                .eq(Spot::getIsDeleted, 0)
                .ge(Spot::getCreatedAt, yesterdayStart)
                .lt(Spot::getCreatedAt, todayStart)
        ));

        appendRecentSeries(response);

        return response;
    }

    @Override
    public OrderTrendResponse getOrderTrend(Integer days, String mode) {
        if (!"range".equalsIgnoreCase(mode) && !"weekday".equalsIgnoreCase(mode)) {
            mode = "weekday";
        }

        Integer normalizedDays = days;
        if (normalizedDays == null) {
            normalizedDays = "weekday".equalsIgnoreCase(mode) ? 0 : 7;
        }

        LambdaQueryWrapper<Order> queryWrapper = new LambdaQueryWrapper<Order>()
            .eq(Order::getIsDeleted, 0)
            .ne(Order::getStatus, OrderStatus.CANCELLED.getCode());

        if (normalizedDays > 0) {
            LocalDate startDate = LocalDate.now().minusDays(normalizedDays - 1L);
            queryWrapper.ge(Order::getCreatedAt, startDate.atStartOfDay());
        }

        List<Order> orders = orderMapper.selectList(queryWrapper);

        if ("weekday".equalsIgnoreCase(mode)) {
            return buildWeekdayTrendResponse(orders);
        }

        return buildRangeTrendResponse(normalizedDays, orders);
    }

    @Override
    public OrderHeatmapResponse getOrderHeatmap(Integer year) {
        int targetYear = year == null ? LocalDate.now().getYear() : year;
        LocalDate startDate = LocalDate.of(targetYear, 1, 1);
        LocalDate endDate = startDate.withMonth(12).withDayOfMonth(31);

        List<Order> orders = orderMapper.selectList(
            new LambdaQueryWrapper<Order>()
                .eq(Order::getIsDeleted, 0)
                .ge(Order::getCreatedAt, startDate.atStartOfDay())
                .lt(Order::getCreatedAt, endDate.plusDays(1).atStartOfDay())
                .ne(Order::getStatus, OrderStatus.CANCELLED.getCode())
        );

        Map<LocalDate, Long> orderMap = orders.stream()
            .collect(Collectors.groupingBy(order -> order.getCreatedAt().toLocalDate(), Collectors.counting()));

        List<OrderHeatmapResponse.HeatmapItem> list = new ArrayList<>();
        long totalDays = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        for (long i = 0; i < totalDays; i++) {
            LocalDate date = startDate.plusDays(i);
            list.add(new OrderHeatmapResponse.HeatmapItem(
                date.format(DateTimeFormatter.ISO_DATE),
                orderMap.getOrDefault(date, 0L)
            ));
        }

        OrderHeatmapResponse response = new OrderHeatmapResponse();
        response.setYear(targetYear);
        response.setList(list);
        return response;
    }

    /**
     * 周内聚合默认使用周一到周日固定顺序，避免和最近 7 天口径混淆。
     */
    private OrderTrendResponse buildWeekdayTrendResponse(List<Order> orders) {
        Map<DayOfWeek, List<Order>> ordersByWeekday = orders.stream()
            .collect(Collectors.groupingBy(order -> order.getCreatedAt().getDayOfWeek()));

        List<OrderTrendResponse.TrendItem> list = new ArrayList<>();
        DayOfWeek[] weekdayOrder = {
            DayOfWeek.MONDAY,
            DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY,
            DayOfWeek.SATURDAY,
            DayOfWeek.SUNDAY
        };

        for (DayOfWeek dayOfWeek : weekdayOrder) {
            List<Order> dayOrders = ordersByWeekday.getOrDefault(dayOfWeek, Collections.emptyList());
            OrderTrendResponse.TrendItem item = new OrderTrendResponse.TrendItem();
            item.setDate(resolveWeekdayLabel(dayOfWeek));
            item.setOrderCount((long) dayOrders.size());
            item.setRevenue(sumRevenue(dayOrders));
            list.add(item);
        }

        OrderTrendResponse response = new OrderTrendResponse();
        response.setList(list);
        return response;
    }

    /**
     * 日期区间统计按自然日补齐空值，保证折线在任意跨度下连续。
     */
    private OrderTrendResponse buildRangeTrendResponse(Integer days, List<Order> orders) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = resolveRangeStartDate(days, orders, endDate);
        long totalDays = ChronoUnit.DAYS.between(startDate, endDate) + 1;

        // 先按日期聚合，再补齐空日期，保证前端折线连续。
        Map<String, List<Order>> ordersByDate = orders.stream()
            .collect(Collectors.groupingBy(o -> 
                o.getCreatedAt().toLocalDate().format(DateTimeFormatter.ISO_DATE)
            ));

        List<OrderTrendResponse.TrendItem> list = new ArrayList<>();
        for (long i = 0; i < totalDays; i++) {
            LocalDate date = startDate.plusDays(i);
            String dateStr = date.format(DateTimeFormatter.ISO_DATE);
            List<Order> dayOrders = ordersByDate.getOrDefault(dateStr, Collections.emptyList());

            OrderTrendResponse.TrendItem item = new OrderTrendResponse.TrendItem();
            item.setDate(dateStr);
            item.setOrderCount((long) dayOrders.size());
            item.setRevenue(sumRevenue(dayOrders));
            list.add(item);
        }

        OrderTrendResponse response = new OrderTrendResponse();
        response.setList(list);
        return response;
    }

    /**
     * 日期趋势支持固定天数，也支持从首笔订单到今天的完整区间。
     */
    private LocalDate resolveRangeStartDate(Integer days, List<Order> orders, LocalDate endDate) {
        if (days != null && days > 0) {
            return endDate.minusDays(days - 1L);
        }

        return orders.stream()
            .map(order -> order.getCreatedAt().toLocalDate())
            .min(LocalDate::compareTo)
            .orElse(endDate);
    }

    /**
     * 仪表盘展示统一使用中文星期文案，避免周统计与近 7 天被误读为同一概念。
     */
    private String resolveWeekdayLabel(DayOfWeek dayOfWeek) {
        return switch (dayOfWeek) {
            case MONDAY -> "周一";
            case TUESDAY -> "周二";
            case WEDNESDAY -> "周三";
            case THURSDAY -> "周四";
            case FRIDAY -> "周五";
            case SATURDAY -> "周六";
            case SUNDAY -> "周日";
        };
    }

    /**
     * 顶部卡片使用最近 10 天真实序列，统一在概览接口返回，避免前端再做多次拼装请求。
     */
    private void appendRecentSeries(DashboardOverviewResponse response) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(9);
        LocalDateTime startTime = startDate.atStartOfDay();

        List<Order> recentOrders = orderMapper.selectList(
            new LambdaQueryWrapper<Order>()
                .eq(Order::getIsDeleted, 0)
                .ge(Order::getCreatedAt, startTime)
                .ne(Order::getStatus, OrderStatus.CANCELLED.getCode())
        );
        List<User> recentUsers = userMapper.selectList(
            new LambdaQueryWrapper<User>()
                .eq(User::getIsDeleted, 0)
                .ge(User::getCreatedAt, startTime)
        );
        List<Spot> recentSpots = spotMapper.selectList(
            new LambdaQueryWrapper<Spot>()
                .eq(Spot::getIsPublished, 1)
                .eq(Spot::getIsDeleted, 0)
                .ge(Spot::getCreatedAt, startTime)
        );

        Map<LocalDate, List<Order>> orderMap = recentOrders.stream()
            .collect(Collectors.groupingBy(order -> order.getCreatedAt().toLocalDate()));
        Map<LocalDate, Long> userMap = recentUsers.stream()
            .collect(Collectors.groupingBy(user -> user.getCreatedAt().toLocalDate(), Collectors.counting()));
        Map<LocalDate, Long> spotMap = recentSpots.stream()
            .collect(Collectors.groupingBy(spot -> spot.getCreatedAt().toLocalDate(), Collectors.counting()));

        List<BigDecimal> revenueSeries = new ArrayList<>();
        List<Long> orderSeries = new ArrayList<>();
        List<Long> userSeries = new ArrayList<>();
        List<Long> spotSeries = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            LocalDate date = startDate.plusDays(i);
            List<Order> dayOrders = orderMap.getOrDefault(date, Collections.emptyList());
            revenueSeries.add(sumRevenue(dayOrders));
            orderSeries.add((long) dayOrders.size());
            userSeries.add(userMap.getOrDefault(date, 0L));
            spotSeries.add(spotMap.getOrDefault(date, 0L));
        }

        response.setRecentRevenueSeries(revenueSeries);
        response.setRecentOrderSeries(orderSeries);
        response.setRecentUserSeries(userSeries);
        response.setRecentSpotSeries(spotSeries);
    }

    // 热门景点统计

    @Override
    public HotSpotsResponse getHotSpots(Integer limit) {
        if (limit == null || limit <= 0) limit = 10;

        // 统计订单数量 (排除已取消的订单)
        List<Order> orders = orderMapper.selectList(
            new LambdaQueryWrapper<Order>()
                .eq(Order::getIsDeleted, 0)
                .ne(Order::getStatus, OrderStatus.CANCELLED.getCode())
        );

        Map<Long, List<Order>> ordersBySpot = orders.stream()
            .collect(Collectors.groupingBy(Order::getSpotId));

        // 获取景点信息
        List<Spot> spots = spotMapper.selectList(
            new LambdaQueryWrapper<Spot>()
                .eq(Spot::getIsPublished, 1)
                .eq(Spot::getIsDeleted, 0)
        );

        Map<Long, Spot> spotMap = spots.stream()
            .collect(Collectors.toMap(Spot::getId, s -> s));

        // 构建热门景点列表
        List<HotSpotsResponse.SpotItem> list = ordersBySpot.entrySet().stream()
            .map(entry -> {
                Long spotId = entry.getKey();
                List<Order> spotOrders = entry.getValue();
                Spot spot = spotMap.get(spotId);

                HotSpotsResponse.SpotItem item = new HotSpotsResponse.SpotItem();
                item.setId(spotId);
                item.setName(spot != null ? spot.getName() : ResourceDisplayText.Spot.UNKNOWN);
                item.setOrderCount((long) spotOrders.size());
                item.setRevenue(sumRevenue(spotOrders));
                item.setAvgRating(spot != null ? spot.getAvgRating() : BigDecimal.ZERO);
                return item;
            })
            .sorted((a, b) -> Long.compare(b.getOrderCount(), a.getOrderCount()))
            .limit(limit)
            .collect(Collectors.toList());

        HotSpotsResponse response = new HotSpotsResponse();
        response.setList(list);
        return response;
    }

    /**
     * 仪表板多个指标都复用同一套营收口径，统一收口后避免某处漏掉状态过滤。
     */
    private BigDecimal sumRevenue(List<Order> orders) {
        return orders.stream()
            .filter(order -> {
                OrderStatus status = OrderStatus.fromCode(order.getStatus());
                return status != null && status.hasRevenue();
            })
            .map(Order::getTotalAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}

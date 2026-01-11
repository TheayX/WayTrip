package com.travel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.travel.dto.dashboard.*;
import com.travel.entity.*;
import com.travel.mapper.*;
import com.travel.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final UserMapper userMapper;
    private final SpotMapper spotMapper;
    private final OrderMapper orderMapper;

    @Override
    public DashboardOverviewResponse getOverview() {
        DashboardOverviewResponse response = new DashboardOverviewResponse();

        // 总用户数
        response.setTotalUsers(userMapper.selectCount(null));

        // 总景点数
        response.setTotalSpots(spotMapper.selectCount(
            new LambdaQueryWrapper<Spot>().eq(Spot::getPublished, 1)
        ));

        // 总订单数和收入 (排除已取消的订单)
        List<Order> allOrders = orderMapper.selectList(
            new LambdaQueryWrapper<Order>().ne(Order::getStatus, Order.STATUS_CANCELLED)
        );
        response.setTotalOrders((long) allOrders.size());
        response.setTotalRevenue(allOrders.stream()
            .filter(o -> o.getStatus() == Order.STATUS_PAID || o.getStatus() == Order.STATUS_REFUNDED)
            .map(Order::getTotalAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add));

        // 今日数据
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        
        List<Order> todayOrders = orderMapper.selectList(
            new LambdaQueryWrapper<Order>()
                .ge(Order::getCreatedAt, todayStart)
                .ne(Order::getStatus, Order.STATUS_CANCELLED)
        );
        response.setTodayOrders((long) todayOrders.size());
        response.setTodayRevenue(todayOrders.stream()
            .filter(o -> o.getStatus() == Order.STATUS_PAID || o.getStatus() == Order.STATUS_REFUNDED)
            .map(Order::getTotalAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add));

        response.setTodayNewUsers(userMapper.selectCount(
            new LambdaQueryWrapper<User>().ge(User::getCreatedAt, todayStart)
        ));

        return response;
    }

    @Override
    public OrderTrendResponse getOrderTrend(Integer days) {
        if (days == null || days <= 0) days = 7;

        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days - 1);

        List<Order> orders = orderMapper.selectList(
            new LambdaQueryWrapper<Order>()
                .ge(Order::getCreatedAt, startDate.atStartOfDay())
                .ne(Order::getStatus, Order.STATUS_CANCELLED)
        );

        // 按日期分组
        Map<String, List<Order>> ordersByDate = orders.stream()
            .collect(Collectors.groupingBy(o -> 
                o.getCreatedAt().toLocalDate().format(DateTimeFormatter.ISO_DATE)
            ));

        // 生成趋势数据
        List<OrderTrendResponse.TrendItem> list = new ArrayList<>();
        for (int i = 0; i < days; i++) {
            LocalDate date = startDate.plusDays(i);
            String dateStr = date.format(DateTimeFormatter.ISO_DATE);
            List<Order> dayOrders = ordersByDate.getOrDefault(dateStr, Collections.emptyList());

            OrderTrendResponse.TrendItem item = new OrderTrendResponse.TrendItem();
            item.setDate(dateStr);
            item.setOrderCount((long) dayOrders.size());
            item.setRevenue(dayOrders.stream()
                .filter(o -> o.getStatus() == Order.STATUS_PAID || o.getStatus() == Order.STATUS_REFUNDED)
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
            list.add(item);
        }

        OrderTrendResponse response = new OrderTrendResponse();
        response.setList(list);
        return response;
    }

    @Override
    public HotSpotsResponse getHotSpots(Integer limit) {
        if (limit == null || limit <= 0) limit = 10;

        // 统计订单数量 (排除已取消的订单)
        List<Order> orders = orderMapper.selectList(
            new LambdaQueryWrapper<Order>().ne(Order::getStatus, Order.STATUS_CANCELLED)
        );

        Map<Long, List<Order>> ordersBySpot = orders.stream()
            .collect(Collectors.groupingBy(Order::getSpotId));

        // 获取景点信息
        List<Spot> spots = spotMapper.selectList(
            new LambdaQueryWrapper<Spot>().eq(Spot::getPublished, 1)
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
                item.setName(spot != null ? spot.getName() : "未知景点");
                item.setOrderCount((long) spotOrders.size());
                item.setRevenue(spotOrders.stream()
                    .filter(o -> o.getStatus() == Order.STATUS_PAID || o.getStatus() == Order.STATUS_REFUNDED)
                    .map(Order::getTotalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add));
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
}

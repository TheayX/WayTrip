package com.travel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.travel.dto.order.*;
import com.travel.entity.Order;
import com.travel.entity.Spot;
import com.travel.entity.User;
import com.travel.enums.OrderStatus;
import com.travel.mapper.OrderMapper;
import com.travel.mapper.SpotMapper;
import com.travel.mapper.UserMapper;
import com.travel.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderMapper orderMapper;
    private final SpotMapper spotMapper;
    private final UserMapper userMapper;

    @Override
    @Transactional
    public OrderDetailResponse createOrder(Long userId, CreateOrderRequest request) {
        Spot spot = spotMapper.selectById(request.getSpotId());
        if (spot == null || spot.getIsDeleted() == 1) {
            throw new RuntimeException("景点不存在");
        }
        if (spot.getIsPublished() != 1) {
            throw new RuntimeException("景点已下架");
        }

        Order order = new Order();
        order.setOrderNo(generateOrderNo());
        order.setUserId(userId);
        order.setSpotId(spot.getId());
        order.setQuantity(request.getQuantity());
        order.setTotalAmount(spot.getPrice().multiply(new java.math.BigDecimal(request.getQuantity())));
        order.setVisitDate(request.getVisitDate());
        order.setContactName(request.getContactName());
        order.setContactPhone(request.getContactPhone());
        order.setStatus(OrderStatus.PENDING.getCode());

        orderMapper.insert(order);

        order.setSpotName(spot.getName());
        order.setSpotImage(spot.getCoverImageUrl());
        order.setUnitPrice(spot.getPrice());

        return buildOrderDetail(order);
    }

    @Override
    public OrderListResponse getUserOrders(Long userId, OrderListRequest request) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getUserId, userId);
        wrapper.eq(Order::getIsDeleted, 0);

        if (request.getStatus() != null && !request.getStatus().isEmpty()) {
            OrderStatus statusEnum = OrderStatus.fromKey(request.getStatus());
            if (statusEnum != null) {
                wrapper.eq(Order::getStatus, statusEnum.getCode());
            }
        }

        wrapper.orderByDesc(Order::getCreatedAt);

        Page<Order> page = new Page<>(request.getPage(), request.getPageSize());
        Page<Order> result = orderMapper.selectPage(page, wrapper);

        fillSpotInfo(result.getRecords());

        OrderListResponse response = new OrderListResponse();
        response.setList(result.getRecords().stream()
            .map(this::buildOrderItem)
            .collect(Collectors.toList()));
        response.setTotal(result.getTotal());
        response.setPage(request.getPage());
        response.setPageSize(request.getPageSize());

        return response;
    }

    @Override
    public OrderDetailResponse getOrderDetail(Long userId, Long orderId) {
        Order order = orderMapper.selectOne(
            new LambdaQueryWrapper<Order>()
                .eq(Order::getId, orderId)
                .eq(Order::getUserId, userId)
                .eq(Order::getIsDeleted, 0)
        );

        if (order == null) {
            throw new RuntimeException("订单不存在");
        }

        fillSpotInfoSingle(order);
        return buildOrderDetail(order);
    }

    @Override
    @Transactional
    public OrderDetailResponse payOrder(Long userId, Long orderId, String idempotentKey) {
        Order order = orderMapper.selectOne(
            new LambdaQueryWrapper<Order>()
                .eq(Order::getId, orderId)
                .eq(Order::getUserId, userId)
                .eq(Order::getIsDeleted, 0)
        );

        if (order == null) {
            throw new RuntimeException("订单不存在");
        }

        if (order.getStatus() == OrderStatus.PAID.getCode()) {
            fillSpotInfoSingle(order);
            return buildOrderDetail(order);
        }

        if (order.getStatus() != OrderStatus.PENDING.getCode()) {
            throw new RuntimeException("订单状态不允许支付");
        }

        order.setStatus(OrderStatus.PAID.getCode());
        order.setPaidAt(LocalDateTime.now());
        orderMapper.updateById(order);

        fillSpotInfoSingle(order);
        return buildOrderDetail(order);
    }

    @Override
    @Transactional
    public OrderDetailResponse cancelOrder(Long userId, Long orderId) {
        Order order = orderMapper.selectOne(
            new LambdaQueryWrapper<Order>()
                .eq(Order::getId, orderId)
                .eq(Order::getUserId, userId)
                .eq(Order::getIsDeleted, 0)
        );

        if (order == null) {
            throw new RuntimeException("订单不存在");
        }

        if (order.getStatus() == OrderStatus.CANCELLED.getCode()) {
            fillSpotInfoSingle(order);
            return buildOrderDetail(order);
        }

        if (order.getStatus() != OrderStatus.PENDING.getCode()) {
            throw new RuntimeException("订单状态不允许取消");
        }

        order.setStatus(OrderStatus.CANCELLED.getCode());
        order.setCancelledAt(LocalDateTime.now());
        orderMapper.updateById(order);

        fillSpotInfoSingle(order);
        return buildOrderDetail(order);
    }


    @Override
    public AdminOrderListResponse getAdminOrders(AdminOrderListRequest request) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getIsDeleted, 0);

        if (request.getOrderNo() != null && !request.getOrderNo().isEmpty()) {
            wrapper.like(Order::getOrderNo, request.getOrderNo());
        }
        
        if (request.getSpotName() != null && !request.getSpotName().isEmpty()) {
            List<Spot> matchingSpots = spotMapper.selectList(
                new LambdaQueryWrapper<Spot>().like(Spot::getName, request.getSpotName())
            );
            if (matchingSpots.isEmpty()) {
                AdminOrderListResponse response = new AdminOrderListResponse();
                response.setList(Collections.emptyList());
                response.setTotal(0L);
                response.setPage(request.getPage());
                response.setPageSize(request.getPageSize());
                return response;
            }
            Set<Long> spotIds = matchingSpots.stream()
                .map(Spot::getId)
                .collect(Collectors.toSet());
            wrapper.in(Order::getSpotId, spotIds);
        }
        
        if (request.getStatus() != null && !request.getStatus().isEmpty()) {
            OrderStatus statusEnum = OrderStatus.fromKey(request.getStatus());
            if (statusEnum != null) {
                wrapper.eq(Order::getStatus, statusEnum.getCode());
            }
        }
        if (request.getStartDate() != null) {
            wrapper.ge(Order::getCreatedAt, request.getStartDate().atStartOfDay());
        }
        if (request.getEndDate() != null) {
            wrapper.le(Order::getCreatedAt, request.getEndDate().atTime(23, 59, 59));
        }

        wrapper.orderByDesc(Order::getCreatedAt);

        Page<Order> page = new Page<>(request.getPage(), request.getPageSize());
        Page<Order> result = orderMapper.selectPage(page, wrapper);

        fillSpotInfo(result.getRecords());

        AdminOrderListResponse response = new AdminOrderListResponse();
        response.setList(result.getRecords().stream()
            .map(this::buildAdminOrderItem)
            .collect(Collectors.toList()));
        response.setTotal(result.getTotal());
        response.setPage(request.getPage());
        response.setPageSize(request.getPageSize());

        return response;
    }

    @Override
    public OrderDetailResponse getAdminOrderDetail(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null || order.getIsDeleted() == 1) {
            throw new RuntimeException("订单不存在");
        }
        fillSpotInfoSingle(order);
        return buildOrderDetail(order);
    }

    @Override
    @Transactional
    public OrderDetailResponse completeOrder(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null || order.getIsDeleted() == 1) {
            throw new RuntimeException("订单不存在");
        }
        if (order.getStatus() == OrderStatus.COMPLETED.getCode()) {
            fillSpotInfoSingle(order);
            return buildOrderDetail(order);
        }
        if (order.getStatus() != OrderStatus.PAID.getCode()) {
            throw new RuntimeException("订单状态不允许完成");
        }
        order.setStatus(OrderStatus.COMPLETED.getCode());
        order.setCompletedAt(LocalDateTime.now());
        orderMapper.updateById(order);
        fillSpotInfoSingle(order);
        return buildOrderDetail(order);
    }

    @Override
    @Transactional
    public OrderDetailResponse refundOrder(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null || order.getIsDeleted() == 1) {
            throw new RuntimeException("订单不存在");
        }
        if (order.getStatus() == OrderStatus.REFUNDED.getCode()) {
            fillSpotInfoSingle(order);
            return buildOrderDetail(order);
        }
        if (order.getStatus() != OrderStatus.PAID.getCode()) {
            throw new RuntimeException("订单状态不允许退款");
        }
        order.setStatus(OrderStatus.REFUNDED.getCode());
        order.setRefundedAt(LocalDateTime.now());
        orderMapper.updateById(order);
        fillSpotInfoSingle(order);
        return buildOrderDetail(order);
    }

    @Override
    @Transactional
    public OrderDetailResponse cancelOrderByAdmin(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null || order.getIsDeleted() == 1) {
            throw new RuntimeException("订单不存在");
        }
        if (order.getStatus() == OrderStatus.CANCELLED.getCode()) {
            fillSpotInfoSingle(order);
            return buildOrderDetail(order);
        }
        if (order.getStatus() != OrderStatus.PENDING.getCode()) {
            throw new RuntimeException("订单状态不允许取消");
        }
        order.setStatus(OrderStatus.CANCELLED.getCode());
        order.setCancelledAt(LocalDateTime.now());
        orderMapper.updateById(order);
        fillSpotInfoSingle(order);
        return buildOrderDetail(order);
    }

    @Override
    @Transactional
    public OrderDetailResponse reopenOrder(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null || order.getIsDeleted() == 1) {
            throw new RuntimeException("订单不存在");
        }
        if (order.getStatus() != OrderStatus.COMPLETED.getCode()) {
            throw new RuntimeException("订单状态不允许恢复");
        }
        UpdateWrapper<Order> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id", orderId)
                .set("status", OrderStatus.PAID.getCode())
                .set("completed_at", null)
                .set("updated_at", LocalDateTime.now());
        orderMapper.update(null, updateWrapper);
        order.setStatus(OrderStatus.PAID.getCode());
        order.setCompletedAt(null);
        fillSpotInfoSingle(order);
        return buildOrderDetail(order);
    }

    private String generateOrderNo() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String random = String.format("%04d", new Random().nextInt(10000));
        return "T" + timestamp + random;
    }

    private String convertStatusToString(Integer status) {
        OrderStatus orderStatus = OrderStatus.fromCode(status);
        return orderStatus != null ? orderStatus.getKey() : "unknown";
    }

    private String getStatusText(Integer status) {
        OrderStatus orderStatus = OrderStatus.fromCode(status);
        return orderStatus != null ? orderStatus.getDescription() : "未知";
    }


    private void fillSpotInfo(List<Order> orders) {
        if (orders == null || orders.isEmpty()) return;

        Set<Long> spotIds = orders.stream()
            .map(Order::getSpotId)
            .collect(Collectors.toSet());

        if (spotIds.isEmpty()) return;

        List<Spot> spots = spotMapper.selectBatchIds(spotIds);
        Map<Long, Spot> spotMap = spots.stream()
            .filter(spot -> spot.getIsDeleted() == 0)
            .collect(Collectors.toMap(Spot::getId, s -> s));

        for (Order order : orders) {
            Spot spot = spotMap.get(order.getSpotId());
            if (spot != null) {
                order.setSpotName(spot.getName());
                order.setSpotImage(spot.getCoverImageUrl());
                order.setUnitPrice(spot.getPrice());
            }
        }
    }

    private void fillSpotInfoSingle(Order order) {
        if (order == null || order.getSpotId() == null) return;
        Spot spot = spotMapper.selectById(order.getSpotId());
        if (spot != null && spot.getIsDeleted() == 0) {
            order.setSpotName(spot.getName());
            order.setSpotImage(spot.getCoverImageUrl());
            order.setUnitPrice(spot.getPrice());
        }
    }

    private OrderListResponse.OrderItem buildOrderItem(Order order) {
        OrderListResponse.OrderItem item = new OrderListResponse.OrderItem();
        item.setId(order.getId());
        item.setOrderNo(order.getOrderNo());
        item.setSpotId(order.getSpotId());
        item.setSpotName(order.getSpotName());
        item.setSpotImage(order.getSpotImage());
        item.setUnitPrice(order.getUnitPrice());
        item.setQuantity(order.getQuantity());
        item.setTotalPrice(order.getTotalAmount());
        item.setVisitDate(order.getVisitDate());
        item.setStatus(convertStatusToString(order.getStatus()));
        item.setStatusText(getStatusText(order.getStatus()));
        item.setCreatedAt(order.getCreatedAt());
        return item;
    }

    private AdminOrderListResponse.OrderItem buildAdminOrderItem(Order order) {
        AdminOrderListResponse.OrderItem item = new AdminOrderListResponse.OrderItem();
        item.setId(order.getId());
        item.setOrderNo(order.getOrderNo());
        item.setUserId(order.getUserId());
        item.setSpotId(order.getSpotId());
        item.setSpotName(order.getSpotName());
        item.setUnitPrice(order.getUnitPrice());
        item.setQuantity(order.getQuantity());
        item.setTotalPrice(order.getTotalAmount());
        item.setVisitDate(order.getVisitDate());
        item.setContactName(order.getContactName());
        item.setContactPhone(order.getContactPhone());
        item.setStatus(convertStatusToString(order.getStatus()));
        item.setStatusText(getStatusText(order.getStatus()));
        item.setPaidAt(order.getPaidAt());
        item.setCompletedAt(order.getCompletedAt());
        item.setCancelledAt(order.getCancelledAt());
        item.setRefundedAt(order.getRefundedAt());
        item.setCreatedAt(order.getCreatedAt());
        item.setUpdatedAt(order.getUpdatedAt());

        User user = userMapper.selectById(order.getUserId());
        if (user != null) {
            item.setUserNickname(user.getNickname());
        }

        return item;
    }

    private OrderDetailResponse buildOrderDetail(Order order) {
        OrderDetailResponse response = new OrderDetailResponse();
        response.setId(order.getId());
        response.setOrderNo(order.getOrderNo());
        response.setSpotId(order.getSpotId());
        response.setSpotName(order.getSpotName());
        response.setSpotImage(order.getSpotImage());
        response.setUnitPrice(order.getUnitPrice());
        response.setQuantity(order.getQuantity());
        response.setTotalPrice(order.getTotalAmount());
        response.setVisitDate(order.getVisitDate());
        response.setContactName(order.getContactName());
        response.setContactPhone(order.getContactPhone());
        response.setStatus(convertStatusToString(order.getStatus()));
        response.setStatusText(getStatusText(order.getStatus()));
        response.setPaidAt(order.getPaidAt());
        response.setCancelledAt(order.getCancelledAt());
        response.setCompletedAt(order.getCompletedAt());
        response.setRefundedAt(order.getRefundedAt());
        response.setCreatedAt(order.getCreatedAt());
        response.setUpdatedAt(order.getUpdatedAt());

        OrderStatus orderStatus = OrderStatus.fromCode(order.getStatus());
        response.setCanPay(orderStatus != null && orderStatus.canPay());
        response.setCanCancel(orderStatus != null && orderStatus.canCancel());

        return response;
    }
}






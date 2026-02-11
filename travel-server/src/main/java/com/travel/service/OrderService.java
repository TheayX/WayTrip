package com.travel.service;

import com.travel.dto.order.*;

/**
 * 订单服务接口
 */
public interface OrderService {

    /**
     * 创建订单
     */
    OrderDetailResponse createOrder(Long userId, CreateOrderRequest request);

    /**
     * 获取用户订单列表
     */
    OrderListResponse getUserOrders(Long userId, OrderListRequest request);

    /**
     * 获取订单详情
     */
    OrderDetailResponse getOrderDetail(Long userId, Long orderId);

    /**
     * 模拟支付
     */
    OrderDetailResponse payOrder(Long userId, Long orderId, String idempotentKey);

    /**
     * 取消订单
     */
    OrderDetailResponse cancelOrder(Long userId, Long orderId);

    /**
     * 管理端获取订单列表
     */
    AdminOrderListResponse getAdminOrders(AdminOrderListRequest request);

    /**
     * 管理端获取订单详情
     */
    OrderDetailResponse getAdminOrderDetail(Long orderId);

    /**
     * 管理端完成订单
     */
    OrderDetailResponse completeOrder(Long orderId);

    /**
     * 管理端退款订单
     */
    OrderDetailResponse refundOrder(Long orderId);
}


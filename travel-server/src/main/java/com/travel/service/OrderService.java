package com.travel.service;

import com.travel.dto.order.request.AdminOrderListRequest;
import com.travel.dto.order.request.CreateOrderRequest;
import com.travel.dto.order.request.OrderListRequest;
import com.travel.dto.order.response.AdminOrderListResponse;
import com.travel.dto.order.response.OrderDetailResponse;
import com.travel.dto.order.response.OrderListResponse;

/**
 * 订单服务接口。
 * <p>
 * 定义用户端订单创建、查询、支付、取消，以及管理端订单处理能力。
 */
public interface OrderService {

    /**
     * 创建订单。
     *
     * @param userId 当前登录用户 ID
     * @param request 订单创建参数
     * @return 订单详情
     */
    OrderDetailResponse createOrder(Long userId, CreateOrderRequest request);

    /**
     * 分页获取当前用户的订单列表。
     *
     * @param userId 当前登录用户 ID
     * @param request 订单查询参数
     * @return 用户订单列表
     */
    OrderListResponse getUserOrders(Long userId, OrderListRequest request);

    /**
     * 获取当前用户的订单详情。
     *
     * @param userId 当前登录用户 ID
     * @param orderId 订单 ID
     * @return 订单详情
     */
    OrderDetailResponse getOrderDetail(Long userId, Long orderId);

    /**
     * 执行订单支付。
     *
     * @param userId 当前登录用户 ID
     * @param orderId 订单 ID
     * @param idempotentKey 幂等键
     * @return 支付后的订单详情
     */
    OrderDetailResponse payOrder(Long userId, Long orderId, String idempotentKey);

    /**
     * 取消当前用户的订单。
     *
     * @param userId 当前登录用户 ID
     * @param orderId 订单 ID
     * @return 取消后的订单详情
     */
    OrderDetailResponse cancelOrder(Long userId, Long orderId);

    /**
     * 分页获取管理端订单列表。
     *
     * @param request 管理端订单查询参数
     * @return 管理端订单分页结果
     */
    AdminOrderListResponse getAdminOrders(AdminOrderListRequest request);

    /**
     * 获取管理端订单详情。
     *
     * @param orderId 订单 ID
     * @return 订单详情
     */
    OrderDetailResponse getAdminOrderDetail(Long orderId);

    /**
     * 将订单标记为已完成。
     *
     * @param orderId 订单 ID
     * @return 更新后的订单详情
     */
    OrderDetailResponse completeOrder(Long orderId);

    /**
     * 将订单标记为已退款。
     *
     * @param orderId 订单 ID
     * @return 更新后的订单详情
     */
    OrderDetailResponse refundOrder(Long orderId);

    /**
     * 管理端取消订单。
     *
     * @param orderId 订单 ID
     * @return 更新后的订单详情
     */
    OrderDetailResponse cancelOrderByAdmin(Long orderId);

    /**
     * 将已完成订单恢复为已支付状态。
     *
     * @param orderId 订单 ID
     * @return 更新后的订单详情
     */
    OrderDetailResponse reopenOrder(Long orderId);
}


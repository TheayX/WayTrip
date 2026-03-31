package com.travel.controller.app;

import com.travel.common.result.ApiResponse;
import com.travel.dto.order.request.CreateOrderRequest;
import com.travel.dto.order.request.OrderListRequest;
import com.travel.dto.order.response.OrderDetailResponse;
import com.travel.dto.order.response.OrderListResponse;
import com.travel.service.OrderService;
import com.travel.util.web.UserContextHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 用户端订单控制器，负责下单、支付与订单查询接口。
 */
@Tag(name = "用户端-订单", description = "用户端订单相关接口")
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @Operation(summary = "创建订单")
    @PostMapping
    public ApiResponse<OrderDetailResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        Long userId = UserContextHolder.getUserId();
        return ApiResponse.success(orderService.createOrder(userId, request));
    }

    @Operation(summary = "获取订单列表")
    @GetMapping
    public ApiResponse<OrderListResponse> getOrders(OrderListRequest request) {
        Long userId = UserContextHolder.getUserId();
        return ApiResponse.success(orderService.getUserOrders(userId, request));
    }

    @Operation(summary = "获取订单详情")
    @GetMapping("/{id}")
    public ApiResponse<OrderDetailResponse> getOrderDetail(@PathVariable("id") Long id) {
        Long userId = UserContextHolder.getUserId();
        return ApiResponse.success(orderService.getOrderDetail(userId, id));
    }

    @Operation(summary = "模拟支付")
    @PostMapping("/{id}/pay")
    public ApiResponse<OrderDetailResponse> payOrder(
            @PathVariable("id") Long id,
            @RequestParam(required = false) String idempotentKey) {
        Long userId = UserContextHolder.getUserId();
        return ApiResponse.success(orderService.payOrder(userId, id, idempotentKey));
    }

    @Operation(summary = "取消订单")
    @PostMapping("/{id}/cancel")
    public ApiResponse<OrderDetailResponse> cancelOrder(@PathVariable("id") Long id) {
        Long userId = UserContextHolder.getUserId();
        return ApiResponse.success(orderService.cancelOrder(userId, id));
    }
}

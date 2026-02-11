package com.travel.controller.admin;

import com.travel.common.result.ApiResponse;
import com.travel.dto.order.*;
import com.travel.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "管理端订单接口", description = "管理端订单管理相关接口")
@RestController
@RequestMapping("/api/admin/v1/orders")
@RequiredArgsConstructor
public class AdminOrderController {

    private final OrderService orderService;

    @Operation(summary = "获取订单列表")
    @GetMapping
    public ApiResponse<AdminOrderListResponse> getOrders(AdminOrderListRequest request) {
        return ApiResponse.success(orderService.getAdminOrders(request));
    }

    @Operation(summary = "获取订单详情")
    @GetMapping("/{id}")
    public ApiResponse<OrderDetailResponse> getOrderDetail(@PathVariable("id") Long id) {
        return ApiResponse.success(orderService.getAdminOrderDetail(id));
    }

    @Operation(summary = "完成订单")
    @PostMapping("/{id}/complete")
    public ApiResponse<OrderDetailResponse> completeOrder(@PathVariable("id") Long id) {
        return ApiResponse.success(orderService.completeOrder(id));
    }

    @Operation(summary = "退款订单")
    @PostMapping("/{id}/refund")
    public ApiResponse<OrderDetailResponse> refundOrder(@PathVariable("id") Long id) {
        return ApiResponse.success(orderService.refundOrder(id));
    }
}

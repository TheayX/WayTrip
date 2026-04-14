package com.travel.service.ai.tool;

import com.travel.dto.order.request.OrderListRequest;
import com.travel.dto.order.response.OrderDetailResponse;
import com.travel.dto.order.response.OrderListResponse;
import com.travel.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 订单工具集合。
 */
@Component
@RequiredArgsConstructor
public class OrderAiTools {

    private final OrderService orderService;
    private final AiToolContextHolder aiToolContextHolder;

    /**
     * 获取当前登录用户的订单列表。
     *
     * @param status 订单状态
     * @param limit 返回条数
     * @return 订单列表摘要
     */
    @Tool(description = "获取当前登录用户的订单列表，可按状态筛选")
    public Map<String, Object> getMyOrders(
            @ToolParam(description = "订单状态，可选值：pending、paid、completed、cancelled", required = false) String status,
            @ToolParam(description = "返回条数，建议 3 到 10 之间", required = false) Integer limit) {
        OrderListRequest request = new OrderListRequest();
        request.setStatus(status);
        request.setPage(1);
        request.setPageSize(normalizeLimit(limit, 5));
        OrderListResponse response = orderService.getUserOrders(aiToolContextHolder.requireCurrentUserId(), request);
        return Map.of(
                "total", response.getTotal(),
                "list", simplifyOrderItems(response.getList())
        );
    }

    /**
     * 获取当前登录用户的订单详情。
     *
     * @param orderId 订单 ID
     * @return 订单详情摘要
     */
    @Tool(description = "获取当前登录用户的指定订单详情")
    public Map<String, Object> getOrderDetail(
            @ToolParam(description = "订单 ID", required = true) Long orderId) {
        OrderDetailResponse response = orderService.getOrderDetail(aiToolContextHolder.requireCurrentUserId(), orderId);
        return simplifyOrderDetail(response);
    }

    /**
     * 获取订单售后建议。
     *
     * @param orderId 订单 ID
     * @return 售后建议摘要
     */
    @Tool(description = "根据订单状态生成售后与下一步操作建议")
    public Map<String, Object> getOrderAfterSalePolicy(
            @ToolParam(description = "订单 ID", required = true) Long orderId) {
        OrderDetailResponse detail = orderService.getOrderDetail(aiToolContextHolder.requireCurrentUserId(), orderId);
        return Map.of(
                "orderId", detail.getId(),
                "orderNo", detail.getOrderNo(),
                "status", detail.getStatus(),
                "statusText", detail.getStatusText(),
                "canPay", Boolean.TRUE.equals(detail.getCanPay()),
                "canCancel", Boolean.TRUE.equals(detail.getCanCancel()),
                "ruleNote", "退款金额、到账时间、售后细则必须以订单页和平台规则为准。"
        );
    }

    private int normalizeLimit(Integer limit, int fallback) {
        if (limit == null || limit <= 0) {
            return fallback;
        }
        return Math.min(limit, 10);
    }

    private List<Map<String, Object>> simplifyOrderItems(List<OrderListResponse.OrderItem> list) {
        if (list == null) {
            return List.of();
        }
        return list.stream().map(item -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", item.getId());
            row.put("orderNo", item.getOrderNo());
            row.put("spotId", item.getSpotId());
            row.put("spotName", item.getSpotName());
            row.put("totalPrice", item.getTotalPrice());
            row.put("visitDate", item.getVisitDate());
            row.put("status", item.getStatus());
            row.put("statusText", item.getStatusText());
            row.put("createdAt", item.getCreatedAt());
            return row;
        }).toList();
    }

    private Map<String, Object> simplifyOrderDetail(OrderDetailResponse detail) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", detail.getId());
        row.put("orderNo", detail.getOrderNo());
        row.put("spotId", detail.getSpotId());
        row.put("spotName", detail.getSpotName());
        row.put("visitDate", detail.getVisitDate());
        row.put("quantity", detail.getQuantity());
        row.put("totalPrice", detail.getTotalPrice());
        row.put("status", detail.getStatus());
        row.put("statusText", detail.getStatusText());
        row.put("canPay", detail.getCanPay());
        row.put("canCancel", detail.getCanCancel());
        row.put("createdAt", detail.getCreatedAt());
        return row;
    }
}

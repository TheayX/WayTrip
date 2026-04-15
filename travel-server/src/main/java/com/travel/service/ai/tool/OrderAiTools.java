package com.travel.service.ai.tool;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.travel.dto.order.request.OrderListRequest;
import com.travel.dto.order.response.OrderDetailResponse;
import com.travel.dto.order.response.OrderListResponse;
import com.travel.entity.Order;
import com.travel.mapper.OrderMapper;
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
    private final OrderMapper orderMapper;
    private final AiToolContextHolder aiToolContextHolder;

    /**
     * 获取订单通用说明。
     *
     * @param topic 问题主题
     * @return 通用说明
     */
    @Tool(description = "获取订单状态说明、退款流程和订单页查看要点等通用帮助")
    public Map<String, Object> getOrderSupportGuide(
            @ToolParam(description = "帮助主题，可选值：status、refund、page、general", required = false) String topic) {
        String normalizedTopic = normalizeTopic(topic);
        aiToolContextHolder.addToolTrace(
                "getOrderSupportGuide",
                "order",
                true,
                "已提供订单通用说明，主题为 " + normalizedTopic
        );
        return switch (normalizedTopic) {
            case "status" -> Map.of(
                    "topic", "status",
                    "title", "订单状态说明",
                    "content", List.of(
                            "待支付：订单已创建但尚未完成支付，超时后可能自动取消。",
                            "已支付：订单已支付成功，通常可等待出行或按规则申请售后。",
                            "已取消：订单已取消，无法继续支付或使用。",
                            "已退款：订单已完成退款处理，到账时间以支付渠道为准。",
                            "已完成：订单已核销或行程结束。"
                    )
            );
            case "refund" -> Map.of(
                    "topic", "refund",
                    "title", "退款流程说明",
                    "content", List.of(
                            "先确认订单当前状态是否允许退款或取消。",
                            "进入订单页查看该订单的可执行操作按钮和状态说明。",
                            "如页面允许退款或取消，按页面提示提交申请。",
                            "退款金额、到账时间和售后细则以订单页和平台规则为准。"
                    )
            );
            case "page" -> Map.of(
                    "topic", "page",
                    "title", "订单页查看要点",
                    "content", List.of(
                            "优先查看订单状态、订单号、出行日期和总金额。",
                            "再查看是否有支付、取消、退款等可执行按钮。",
                            "如需售后，关注页面上的退款说明和规则提示。"
                    )
            );
            default -> Map.of(
                    "topic", "general",
                    "title", "订单帮助概览",
                    "content", List.of(
                            "订单问题通常分为状态说明、支付取消、退款售后和具体订单查询。",
                            "通用规则问题不一定需要订单号，具体订单问题建议提供订单号或先查看最近订单。"
                    )
            );
        };
    }

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
        aiToolContextHolder.addToolTrace(
                "getMyOrders",
                "order",
                true,
                "已查询当前登录用户的订单列表，共 " + response.getTotal() + " 条"
        );
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
        aiToolContextHolder.addToolTrace(
                "getOrderDetail",
                "order",
                true,
                "已查询订单详情，订单号为 " + response.getOrderNo()
        );
        return simplifyOrderDetail(response);
    }

    /**
     * 按订单号获取当前登录用户的订单详情。
     *
     * @param orderNo 订单号
     * @return 订单详情摘要
     */
    @Tool(description = "按订单号获取当前登录用户的指定订单详情")
    public Map<String, Object> getOrderDetailByOrderNo(
            @ToolParam(description = "订单号", required = true) String orderNo) {
        Long userId = aiToolContextHolder.requireCurrentUserId();
        Order order = orderMapper.selectOne(new LambdaQueryWrapper<Order>()
                .eq(Order::getUserId, userId)
                .eq(Order::getOrderNo, orderNo == null ? "" : orderNo.trim())
                .eq(Order::getIsDeleted, 0)
                .last("LIMIT 1"));
        if (order == null) {
            aiToolContextHolder.addToolTrace(
                    "getOrderDetailByOrderNo",
                    "order",
                    false,
                    "未找到订单号为 " + orderNo + " 的当前用户订单"
            );
            return Map.of(
                    "found", false,
                    "orderNo", orderNo == null ? "" : orderNo.trim(),
                    "message", "未找到匹配的订单，请确认订单号是否正确，或先查看最近订单。"
            );
        }
        OrderDetailResponse response = orderService.getOrderDetail(userId, order.getId());
        aiToolContextHolder.addToolTrace(
                "getOrderDetailByOrderNo",
                "order",
                true,
                "已按订单号查询订单详情，订单号为 " + response.getOrderNo()
        );
        Map<String, Object> result = simplifyOrderDetail(response);
        result.put("found", true);
        return result;
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
        aiToolContextHolder.addToolTrace(
                "getOrderAfterSalePolicy",
                "order",
                true,
                "已根据订单状态生成售后建议，当前状态为 " + detail.getStatusText()
        );
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

    private String normalizeTopic(String topic) {
        if (topic == null || topic.isBlank()) {
            return "general";
        }
        String normalized = topic.trim().toLowerCase();
        if (normalized.contains("status") || normalized.contains("状态")) {
            return "status";
        }
        if (normalized.contains("refund") || normalized.contains("退款") || normalized.contains("售后")) {
            return "refund";
        }
        if (normalized.contains("page") || normalized.contains("页面") || normalized.contains("订单页")) {
            return "page";
        }
        return "general";
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

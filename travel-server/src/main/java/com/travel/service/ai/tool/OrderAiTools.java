package com.travel.service.ai.tool;

import com.travel.dto.order.request.OrderListRequest;
import com.travel.dto.order.response.OrderDetailResponse;
import com.travel.dto.order.response.OrderListResponse;
import com.travel.service.OrderService;
import com.travel.service.ai.rule.OrderBusinessRuleProvider;
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

    /**
     * 订单服务。
     */
    private final OrderService orderService;

    /**
     * 订单规则提供器。
     */
    private final OrderBusinessRuleProvider orderBusinessRuleProvider;

    /**
     * AI 工具上下文。
     */
    private final AiToolContextHolder aiToolContextHolder;

    /**
     * 获取订单通用说明。
     *
     * @param topic 问题主题
     * @return 通用说明
     */
    @Tool(description = "获取基于系统真实规则整理的订单状态说明、退款流程和订单页查看要点等通用帮助")
    public Map<String, Object> getOrderSupportGuide(
            @ToolParam(description = "帮助主题，可选值：status、refund、page、general", required = false) String topic) {
        String normalizedTopic = normalizeTopic(topic);
        Map<String, Object> rules = orderBusinessRuleProvider.describeRules();
        aiToolContextHolder.addToolTrace(
                "getOrderSupportGuide",
                "order",
                true,
                "已提供订单通用说明，主题为 " + normalizedTopic
        );
        Map<String, Object> data = switch (normalizedTopic) {
            case "status" -> Map.of(
                    "topic", "status",
                    "title", "订单状态说明",
                    "statuses", rules.get("statuses"),
                    "paymentTimeoutMinutes", rules.get("paymentTimeoutMinutes"),
                    "disclaimer", rules.get("disclaimer")
            );
            case "refund" -> Map.of(
                    "topic", "refund",
                    "title", "退款流程说明",
                    "content", rules.get("refundPolicy"),
                    "disclaimer", rules.get("disclaimer")
            );
            case "page" -> Map.of(
                    "topic", "page",
                    "title", "订单页查看要点",
                    "content", rules.get("pageChecklist")
            );
            default -> Map.of(
                    "topic", "general",
                    "title", "订单帮助概览",
                    "content", rules.get("generalOverview"),
                    "paymentTimeoutMinutes", rules.get("paymentTimeoutMinutes"),
                    "disclaimer", rules.get("disclaimer")
            );
        };
        return AiToolResponse.success("已获取订单通用说明", data);
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
            @ToolParam(description = "订单状态，可选值：pending、paid、completed、cancelled、refunded", required = false) String status,
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
        return AiToolResponse.success(
                "已获取当前登录用户的订单列表",
                Map.of(
                        "total", response.getTotal(),
                        "list", simplifyOrderItems(response.getList())
                )
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
        return AiToolResponse.success("已获取订单详情", simplifyOrderDetail(response));
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
        OrderDetailResponse response = orderService.getOrderDetailByOrderNo(userId, orderNo);
        if (response == null) {
            aiToolContextHolder.addToolTrace(
                    "getOrderDetailByOrderNo",
                    "order",
                    false,
                    "未找到订单号为 " + orderNo + " 的当前用户订单"
            );
            return AiToolResponse.failure("未找到匹配的订单，请确认订单号是否正确，或先查看最近订单。");
        }
        aiToolContextHolder.addToolTrace(
                "getOrderDetailByOrderNo",
                "order",
                true,
                "已按订单号查询订单详情，订单号为 " + response.getOrderNo()
        );
        return AiToolResponse.success("已按订单号获取订单详情", simplifyOrderDetail(response));
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
        Map<String, Object> rules = orderBusinessRuleProvider.describeRules();
        aiToolContextHolder.addToolTrace(
                "getOrderAfterSalePolicy",
                "order",
                true,
                "已根据订单状态生成售后建议，当前状态为 " + detail.getStatusText()
        );
        return AiToolResponse.success(
                "已生成订单售后建议",
                Map.of(
                        "orderId", detail.getId(),
                        "orderNo", detail.getOrderNo(),
                        "status", detail.getStatus(),
                        "statusText", detail.getStatusText(),
                        "canPay", Boolean.TRUE.equals(detail.getCanPay()),
                        "canCancel", Boolean.TRUE.equals(detail.getCanCancel()),
                        "ruleNote", rules.get("disclaimer")
                )
        );
    }

    /**
     * 规范化返回条数。
     *
     * @param limit 原始条数
     * @param fallback 默认条数
     * @return 安全条数
     */
    private int normalizeLimit(Integer limit, int fallback) {
        if (limit == null || limit <= 0) {
            return fallback;
        }
        return Math.min(limit, 10);
    }

    /**
     * 将自然语言主题归一到固定的订单帮助主题。
     *
     * @param topic 原始主题
     * @return 归一化主题
     */
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

    /**
     * 将订单列表裁剪为适合模型消费的轻量摘要。
     *
     * @param list 原始订单列表
     * @return 轻量订单列表
     */
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

    /**
     * 将订单详情裁剪为轻量摘要。
     *
     * @param detail 订单详情
     * @return 轻量订单详情
     */
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

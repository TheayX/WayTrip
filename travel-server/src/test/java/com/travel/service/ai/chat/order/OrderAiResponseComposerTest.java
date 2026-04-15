package com.travel.service.ai.chat.order;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 订单 AI 回复生成器测试，确保事实型回复直接、可执行。
 */
class OrderAiResponseComposerTest {

    private final OrderAiResponseComposer composer = new OrderAiResponseComposer();

    @Test
    void composeRefundEligibilityForPaidOrder() {
        String reply = composer.compose(new OrderAiToolResult(
                OrderAiIntent.REFUND_ELIGIBILITY_BY_ORDER_NO,
                Map.of(
                        "found", true,
                        "orderNo", "T202604121416166706",
                        "spotName", "上海迪士尼乐园",
                        "status", "paid",
                        "statusText", "已支付"
                )
        ));

        assertTrue(reply.contains("可以进入退款/售后处理"));
        assertTrue(reply.contains("当前状态：已支付"));
        assertTrue(reply.contains("判断依据"));
    }

    @Test
    void composeOrderListWithRecentItems() {
        String reply = composer.compose(new OrderAiToolResult(
                OrderAiIntent.LIST_ORDERS,
                Map.of(
                        "total", 1,
                        "list", List.of(Map.of(
                                "spotName", "故宫博物院",
                                "statusText", "已支付",
                                "orderNo", "T202604102347254089",
                                "visitDate", "2026-04-20",
                                "totalPrice", "60.00"
                        ))
                )
        ));

        assertTrue(reply.contains("已查询到你的订单，共 1 条"));
        assertTrue(reply.contains("故宫博物院"));
        assertTrue(reply.contains("T202604102347254089"));
    }
}

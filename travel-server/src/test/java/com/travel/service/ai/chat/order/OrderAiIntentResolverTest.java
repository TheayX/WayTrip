package com.travel.service.ai.chat.order;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 订单 AI 意图解析器测试，固定常见自然语言问法到稳定业务动作。
 */
class OrderAiIntentResolverTest {

    private final OrderAiIntentResolver resolver = new OrderAiIntentResolver();

    @Test
    void resolveAllOrdersAsOrderList() {
        OrderAiIntentResult result = resolver.resolve("帮我查看所有订单");

        assertEquals(OrderAiIntent.LIST_ORDERS, result.intent());
        assertEquals(10, result.limit());
    }

    @Test
    void resolveInformalOrderListAsOrderList() {
        OrderAiIntentResult result = resolver.resolve("我的单子都列出来");

        assertEquals(OrderAiIntent.LIST_ORDERS, result.intent());
    }

    @Test
    void resolvePaidOrdersWithStatusSlot() {
        OrderAiIntentResult result = resolver.resolve("查一下已支付订单");

        assertEquals(OrderAiIntent.LIST_ORDERS, result.intent());
        assertEquals("paid", result.status());
    }

    @Test
    void resolveOrderStatusGuideBeforeOrderList() {
        OrderAiIntentResult result = resolver.resolve("帮我看看订单状态说明");

        assertEquals(OrderAiIntent.GUIDE_STATUS, result.intent());
    }

    @Test
    void resolveOrderNoDetail() {
        OrderAiIntentResult result = resolver.resolve("帮我看看订单 T202604150001");

        assertEquals(OrderAiIntent.DETAIL_BY_ORDER_NO, result.intent());
        assertEquals("T202604150001", result.orderNo());
    }

    @Test
    void resolveOrderNoDetailWithoutSpace() {
        OrderAiIntentResult result = resolver.resolve("帮我看看订单T202604121416166706");

        assertEquals(OrderAiIntent.DETAIL_BY_ORDER_NO, result.intent());
        assertEquals("T202604121416166706", result.orderNo());
    }
}

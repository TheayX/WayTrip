package com.travel.service.ai.chat.order;

import com.travel.service.ai.chat.intent.AiIntentClassificationResult;
import com.travel.service.ai.chat.intent.AiIntentSlots;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 订单 AI 意图解析器测试，固定常见自然语言问法到稳定业务动作。
 */
class OrderAiIntentResolverTest {

    private final OrderAiIntentResolver resolver = new OrderAiIntentResolver();

    @Test
    void resolveAllOrdersAsOrderList() {
        AiIntentClassificationResult result = resolver.resolve("帮我查看所有订单");

        assertEquals(OrderAiIntent.LIST_ORDERS.name(), result.intent());
        assertEquals(10, result.slotAsInt(AiIntentSlots.LIMIT, 0));
    }

    @Test
    void resolveInformalOrderListAsOrderList() {
        AiIntentClassificationResult result = resolver.resolve("我的单子都列出来");

        assertEquals(OrderAiIntent.LIST_ORDERS.name(), result.intent());
    }

    @Test
    void resolvePaidOrdersWithStatusSlot() {
        AiIntentClassificationResult result = resolver.resolve("查一下已支付订单");

        assertEquals(OrderAiIntent.LIST_ORDERS.name(), result.intent());
        assertEquals("paid", result.slotAsString(AiIntentSlots.STATUS));
    }

    @Test
    void resolveOrderStatusGuideBeforeOrderList() {
        AiIntentClassificationResult result = resolver.resolve("帮我看看订单状态说明");

        assertEquals(OrderAiIntent.GUIDE_STATUS.name(), result.intent());
    }

    @Test
    void resolveOrderNoDetail() {
        AiIntentClassificationResult result = resolver.resolve("帮我看看订单 T202604150001");

        assertEquals(OrderAiIntent.DETAIL_BY_ORDER_NO.name(), result.intent());
        assertEquals("T202604150001", result.slotAsString(AiIntentSlots.ORDER_NO));
    }

    @Test
    void resolveOrderNoDetailWithoutSpace() {
        AiIntentClassificationResult result = resolver.resolve("帮我看看订单T202604121416166706");

        assertEquals(OrderAiIntent.DETAIL_BY_ORDER_NO.name(), result.intent());
        assertEquals("T202604121416166706", result.slotAsString(AiIntentSlots.ORDER_NO));
    }

    @Test
    void resolveRefundEligibilityWithOrderNo() {
        AiIntentClassificationResult result = resolver.resolve("T202604121416166706这个订单能退款吗");

        assertEquals(OrderAiIntent.REFUND_ELIGIBILITY.name(), result.intent());
        assertEquals("T202604121416166706", result.slotAsString(AiIntentSlots.ORDER_NO));
    }
}

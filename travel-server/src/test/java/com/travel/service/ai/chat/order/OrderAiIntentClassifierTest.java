package com.travel.service.ai.chat.order;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travel.service.ai.chat.intent.AiJsonIntentClassificationSupport;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 订单 AI 意图分类器测试，覆盖模型 JSON 输出的解析与规范化。
 */
class OrderAiIntentClassifierTest {

    private final OrderAiIntentClassifier classifier = new OrderAiIntentClassifier(
            new AiJsonIntentClassificationSupport(null, new ObjectMapper()),
            new OrderAiIntentResolver()
    );

    @Test
    void parsePlainJsonModelReply() throws Exception {
        OrderAiIntentResult result = classifier.parseModelReply("""
                {"intent":"REFUND_ELIGIBILITY_BY_ORDER_NO","orderNo":"T202604121416166706","status":"","limit":10}
                """);

        assertEquals(OrderAiIntent.REFUND_ELIGIBILITY_BY_ORDER_NO, result.intent());
        assertEquals("T202604121416166706", result.orderNo());
    }

    @Test
    void parseMarkdownWrappedJsonModelReply() throws Exception {
        OrderAiIntentResult result = classifier.parseModelReply("""
                ```json
                {"intent":"LIST_ORDERS","orderNo":"","status":"paid","limit":20}
                ```
                """);

        assertEquals(OrderAiIntent.LIST_ORDERS, result.intent());
        assertEquals("paid", result.status());
        assertEquals(10, result.limit());
    }

    @Test
    void parseUnknownIntentAsNone() throws Exception {
        OrderAiIntentResult result = classifier.parseModelReply("""
                {"intent":"UNKNOWN","orderNo":"","status":"","limit":10}
                """);

        assertEquals(OrderAiIntent.NONE, result.intent());
    }
}

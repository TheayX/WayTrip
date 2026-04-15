package com.travel.service.ai.chat.order;

import com.fasterxml.jackson.databind.JsonNode;
import com.travel.service.ai.chat.intent.AiJsonIntentClassificationSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 订单 AI 意图分类器，优先由模型理解自然语言，再由后端校验关键槽位。
 */
@Service
@RequiredArgsConstructor
public class OrderAiIntentClassifier {

    private static final String SYSTEM_PROMPT = """
            你是 WayTrip 订单意图分类器。你的任务是把用户问题分类成一个稳定业务意图，只输出 JSON。
            不要回答用户问题，不要解释，不要输出 Markdown。
            可选 intent：
            - GUIDE_STATUS：询问订单状态含义、状态说明、状态有哪些
            - GUIDE_REFUND：询问通用退款流程、售后流程、退款规则
            - GUIDE_PAGE：询问订单页怎么看、订单页要看什么
            - LIST_ORDERS：查询我的订单列表、全部订单、历史订单、某状态订单
            - DETAIL_BY_ORDER_NO：查询某个订单号的订单详情
            - REFUND_ELIGIBILITY_BY_ORDER_NO：询问某个订单号是否能退款、能否售后、可不可以退
            - NONE：不属于订单模块或意图不足
            输出 JSON 字段：
            {"intent":"...","orderNo":"","status":"","limit":10}
            status 只允许 pending、paid、completed、cancelled、refunded 或空字符串。
            如果用户提供订单号，必须原样填入 orderNo。
            """;

    private final AiJsonIntentClassificationSupport intentClassificationSupport;
    private final OrderAiIntentResolver fallbackResolver;

    /**
     * 分类订单意图。
     *
     * @param userMessage 用户问题
     * @return 分类结果
     */
    public OrderAiIntentResult classify(String userMessage) {
        OrderAiIntentResult fallback = fallbackResolver.resolve(userMessage);
        return intentClassificationSupport.classify(
                "订单 AI",
                SYSTEM_PROMPT,
                userMessage,
                () -> fallback,
                root -> normalizeClassifiedResult(parseModelJson(root), fallback)
        );
    }

    OrderAiIntentResult parseModelReply(String reply) throws Exception {
        return parseModelJson(intentClassificationSupport.parseJsonReply(reply));
    }

    private OrderAiIntentResult parseModelJson(JsonNode root) {
        OrderAiIntent intent = parseIntent(root.path("intent").asText(""));
        String orderNo = root.path("orderNo").asText("");
        String status = normalizeStatus(root.path("status").asText(""));
        int limit = normalizeLimit(root.path("limit").asInt(10));
        return new OrderAiIntentResult(intent, orderNo, status, limit);
    }

    private OrderAiIntentResult normalizeClassifiedResult(OrderAiIntentResult classified, OrderAiIntentResult fallback) {
        if (classified.intent() == OrderAiIntent.NONE) {
            return fallback.intent() == OrderAiIntent.NONE ? classified : fallback;
        }
        String orderNo = StringUtils.hasText(classified.orderNo()) ? classified.orderNo().trim() : fallback.orderNo();
        String status = StringUtils.hasText(classified.status()) ? classified.status() : fallback.status();
        int limit = classified.limit() > 0 ? classified.limit() : fallback.limit();
        OrderAiIntent intent = classified.intent();
        if (requiresOrderNo(intent) && !StringUtils.hasText(orderNo)) {
            return fallback;
        }
        return new OrderAiIntentResult(intent, orderNo, status, normalizeLimit(limit));
    }

    private boolean requiresOrderNo(OrderAiIntent intent) {
        return intent == OrderAiIntent.DETAIL_BY_ORDER_NO || intent == OrderAiIntent.REFUND_ELIGIBILITY_BY_ORDER_NO;
    }

    private OrderAiIntent parseIntent(String value) {
        if (!StringUtils.hasText(value)) {
            return OrderAiIntent.NONE;
        }
        try {
            return OrderAiIntent.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return OrderAiIntent.NONE;
        }
    }

    private String normalizeStatus(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String normalized = value.trim().toLowerCase();
        return switch (normalized) {
            case "pending", "paid", "completed", "cancelled", "refunded" -> normalized;
            default -> null;
        };
    }

    private int normalizeLimit(int value) {
        if (value <= 0) {
            return 10;
        }
        return Math.min(value, 10);
    }

}

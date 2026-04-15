package com.travel.service.ai.chat.order;

import com.fasterxml.jackson.databind.JsonNode;
import com.travel.enums.ai.AiScenarioType;
import com.travel.service.ai.chat.intent.AiIntentClassificationResult;
import com.travel.service.ai.chat.intent.AiIntentSlots;
import com.travel.service.ai.chat.intent.AiJsonIntentClassificationSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;

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
            - REFUND_ELIGIBILITY：询问某个订单号是否能退款、能否售后、可不可以退
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
    public AiIntentClassificationResult classify(String userMessage) {
        AiIntentClassificationResult fallback = fallbackResolver.resolve(userMessage);
        return intentClassificationSupport.classify(
                "订单 AI",
                SYSTEM_PROMPT,
                userMessage,
                () -> fallback,
                root -> normalizeClassifiedResult(parseModelJson(root), fallback)
        );
    }

    AiIntentClassificationResult parseModelReply(String reply) throws Exception {
        return parseModelJson(intentClassificationSupport.parseJsonReply(reply));
    }

    private AiIntentClassificationResult parseModelJson(JsonNode root) {
        OrderAiIntent intent = parseIntent(root.path("intent").asText(""));
        String orderNo = root.path("orderNo").asText("");
        String status = normalizeStatus(root.path("status").asText(""));
        int limit = normalizeLimit(root.path("limit").asInt(10));
        return toIntentPackage(intent, orderNo, status, limit);
    }

    private AiIntentClassificationResult normalizeClassifiedResult(AiIntentClassificationResult classified,
                                                                  AiIntentClassificationResult fallback) {
        OrderAiIntent classifiedIntent = parseIntent(classified.intent());
        if (classifiedIntent == OrderAiIntent.NONE) {
            return OrderAiIntent.NONE.name().equals(fallback.intent()) ? classified : fallback;
        }
        String orderNo = StringUtils.hasText(classified.slotAsString(AiIntentSlots.ORDER_NO))
                ? classified.slotAsString(AiIntentSlots.ORDER_NO)
                : fallback.slotAsString(AiIntentSlots.ORDER_NO);
        String status = StringUtils.hasText(classified.slotAsString(AiIntentSlots.STATUS))
                ? classified.slotAsString(AiIntentSlots.STATUS)
                : fallback.slotAsString(AiIntentSlots.STATUS);
        int limit = classified.slotAsInt(AiIntentSlots.LIMIT, fallback.slotAsInt(AiIntentSlots.LIMIT, 10));
        OrderAiIntent intent = classifiedIntent;
        if (requiresOrderNo(intent) && !StringUtils.hasText(orderNo)) {
            return fallback;
        }
        return toIntentPackage(intent, orderNo, status, normalizeLimit(limit));
    }

    private boolean requiresOrderNo(OrderAiIntent intent) {
        return intent == OrderAiIntent.DETAIL_BY_ORDER_NO || intent == OrderAiIntent.REFUND_ELIGIBILITY;
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

    private AiIntentClassificationResult toIntentPackage(OrderAiIntent intent, String orderNo, String status, int limit) {
        Map<String, Object> slots = new LinkedHashMap<>();
        if (StringUtils.hasText(orderNo)) {
            slots.put(AiIntentSlots.ORDER_NO, orderNo.trim());
        }
        if (StringUtils.hasText(status)) {
            slots.put(AiIntentSlots.STATUS, status);
        }
        if (limit > 0) {
            slots.put(AiIntentSlots.LIMIT, normalizeLimit(limit));
        }
        return new AiIntentClassificationResult(
                AiScenarioType.ORDER_ADVISOR,
                intent.name(),
                slots,
                intent == OrderAiIntent.NONE ? 0D : 0.8D,
                requiresLogin(intent),
                requiresTool(intent)
        );
    }

    private boolean requiresLogin(OrderAiIntent intent) {
        return intent == OrderAiIntent.LIST_ORDERS
                || intent == OrderAiIntent.DETAIL_BY_ORDER_NO
                || intent == OrderAiIntent.REFUND_ELIGIBILITY;
    }

    private boolean requiresTool(OrderAiIntent intent) {
        return intent != OrderAiIntent.NONE;
    }

}

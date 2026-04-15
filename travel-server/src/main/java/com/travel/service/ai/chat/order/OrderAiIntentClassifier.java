package com.travel.service.ai.chat.order;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 订单 AI 意图分类器，优先由模型理解自然语言，再由后端校验关键槽位。
 */
@Slf4j
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

    private final ChatClient aiChatClient;
    private final ObjectMapper objectMapper;
    private final OrderAiIntentResolver fallbackResolver;

    /**
     * 分类订单意图。
     *
     * @param userMessage 用户问题
     * @return 分类结果
     */
    public OrderAiIntentResult classify(String userMessage) {
        OrderAiIntentResult fallback = fallbackResolver.resolve(userMessage);
        if (!StringUtils.hasText(userMessage)) {
            return fallback;
        }
        try {
            String reply = aiChatClient.prompt()
                    .system(SYSTEM_PROMPT)
                    .user("用户问题：" + userMessage.trim())
                    .call()
                    .content();
            OrderAiIntentResult classified = parseModelReply(reply);
            OrderAiIntentResult normalized = normalizeClassifiedResult(classified, fallback);
            log.info(
                    "订单 AI 意图分类完成：模型原始输出={}, 分类意图={}, 订单号={}, 状态={}, 兜底意图={}",
                    preview(reply),
                    normalized.intent(),
                    normalized.orderNo(),
                    normalized.status(),
                    fallback.intent()
            );
            return normalized;
        } catch (Exception e) {
            log.warn("订单 AI 意图分类失败，降级使用规则兜底：用户问题预览={}, 兜底意图={}", preview(userMessage), fallback.intent(), e);
            return fallback;
        }
    }

    OrderAiIntentResult parseModelReply(String reply) throws Exception {
        String json = extractJson(reply);
        JsonNode root = objectMapper.readTree(json);
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

    private String extractJson(String value) {
        if (!StringUtils.hasText(value)) {
            return "{}";
        }
        String trimmed = value.trim();
        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return trimmed.substring(start, end + 1);
        }
        return trimmed;
    }

    private String preview(String value) {
        if (!StringUtils.hasText(value)) {
            return "无";
        }
        String normalized = value.trim().replaceAll("\\s+", " ");
        return normalized.length() <= 120 ? normalized : normalized.substring(0, 120) + "...";
    }
}

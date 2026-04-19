package com.travel.service.ai.intent;

import com.fasterxml.jackson.databind.JsonNode;
import com.travel.enums.ai.AiScenarioType;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 运行时意图识别服务。
 * <p>
 * 主链路只保留这一层统一做意图识别：优先尝试模型 JSON 分类，失败后回退到规则解析。
 */
@Service
public class AiIntentService {

    private static final Pattern ORDER_NO_PATTERN = Pattern.compile("T\\d{8,}");
    private static final Pattern BUDGET_PATTERN = Pattern.compile("预算\\s*(\\d+)");
    private static final Pattern DAY_PATTERN = Pattern.compile("(\\d+)\\s*天|([两二2])日游|([两二2])天");

    private final AiJsonIntentClassificationSupport jsonSupport;

    public AiIntentService(@Qualifier("aiChatClient") ChatClient aiChatClient,
                           com.fasterxml.jackson.databind.ObjectMapper objectMapper) {
        this.jsonSupport = new AiJsonIntentClassificationSupport(aiChatClient, objectMapper);
    }

    /**
     * 根据当前场景做细粒度意图识别。
     *
     * @param userMessage 用户消息
     * @param scenario 当前场景
     * @return 意图识别结果；无法识别时返回 null
     */
    public AiIntentResult recognize(String userMessage, AiScenarioType scenario) {
        if (scenario == null) {
            return null;
        }
        AiIntentResult fallback = recognizeByRule(userMessage, scenario);
        try {
            String raw = jsonSupport.classify(buildSystemPrompt(scenario), userMessage);
            if (!StringUtils.hasText(raw)) {
                return fallback;
            }
            return mergeWithFallback(parseModelResult(raw, scenario), fallback);
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private AiIntentResult parseModelResult(String raw, AiScenarioType scenario) throws Exception {
        JsonNode root = jsonSupport.parseJson(raw);
        Map<String, Object> slots = new LinkedHashMap<>();
        putText(slots, AiIntentSlots.ORDER_NO, root.path("orderNo").asText(""));
        putText(slots, AiIntentSlots.STATUS, root.path("status").asText(""));
        putText(slots, AiIntentSlots.KEYWORD, normalizeKeyword(root.path("keyword").asText(""), scenario));
        putText(slots, AiIntentSlots.SPOT_NAME, root.path("spotName").asText(""));
        putText(slots, AiIntentSlots.CITY, root.path("city").asText(""));
        putText(slots, AiIntentSlots.FACT_FIELD, root.path("factField").asText(""));
        putInt(slots, AiIntentSlots.BUDGET, root.path("budget").asInt(0));
        putInt(slots, AiIntentSlots.DAYS, root.path("days").asInt(0));
        putText(slots, AiIntentSlots.GROUP, root.path("group").asText(""));
        putText(slots, AiIntentSlots.PREFERENCES, root.path("preferences").asText(""));
        long spotId = root.path("spotId").asLong(0L);
        if (spotId > 0) {
            slots.put(AiIntentSlots.SPOT_ID, spotId);
        }
        int limit = root.path("limit").asInt(0);
        if (limit > 0) {
            slots.put(AiIntentSlots.LIMIT, Math.min(limit, 10));
        }
        String intent = root.path("intent").asText("").trim().toUpperCase();
        if (!StringUtils.hasText(intent)) {
            return null;
        }
        return new AiIntentResult(scenario, intent, slots, 0.85D, requiresLogin(scenario), requiresTool(scenario));
    }

    private AiIntentResult mergeWithFallback(AiIntentResult modelResult,
                                             AiIntentResult fallback) {
        if (modelResult == null || !StringUtils.hasText(modelResult.intent())) {
            return fallback;
        }
        if (fallback == null) {
            return modelResult;
        }
        Map<String, Object> mergedSlots = new LinkedHashMap<>(fallback.slots());
        mergedSlots.putAll(modelResult.slots());
        String intent = modelResult.intent().endsWith("NONE") ? fallback.intent() : modelResult.intent();
        return new AiIntentResult(
                fallback.scenario(),
                intent,
                mergedSlots,
                modelResult.confidence(),
                fallback.requiresLogin(),
                fallback.requiresTool()
        );
    }

    private AiIntentResult recognizeByRule(String userMessage, AiScenarioType scenario) {
        String message = StringUtils.hasText(userMessage) ? userMessage.trim() : "";
        return switch (scenario) {
            case ORDER_ADVISOR -> recognizeOrder(message);
            case SPOT_QA, GUIDE_QA -> recognizeTravelContent(message, scenario);
            case TRAVEL_PLANNER -> recognizeTravelPlan(message);
            case RECOMMENDATION_EXPLAINER -> recognizeRecommendation(message);
            case USER_PROFILE_ANALYZER -> recognizeProfile(message);
            case OPERATION_ANALYZER -> recognizeOperation(message);
            default -> null;
        };
    }

    private AiIntentResult recognizeOrder(String message) {
        Map<String, Object> slots = new LinkedHashMap<>();
        slots.put(AiIntentSlots.LIMIT, 10);
        String orderNo = extractOrderNo(message);
        putText(slots, AiIntentSlots.ORDER_NO, orderNo);
        if (message.contains("状态说明")) {
            return new AiIntentResult(AiScenarioType.ORDER_ADVISOR, "GUIDE_STATUS", slots, 0.9D, true, true);
        }
        if ((message.contains("退款") || message.contains("退吗")) && StringUtils.hasText(orderNo)) {
            return new AiIntentResult(AiScenarioType.ORDER_ADVISOR, "REFUND_ELIGIBILITY", slots, 0.9D, true, true);
        }
        if (StringUtils.hasText(orderNo)) {
            return new AiIntentResult(AiScenarioType.ORDER_ADVISOR, "DETAIL_BY_ORDER_NO", slots, 0.9D, true, true);
        }
        if (message.contains("已支付")) {
            slots.put(AiIntentSlots.STATUS, "paid");
            return new AiIntentResult(AiScenarioType.ORDER_ADVISOR, "LIST_ORDERS", slots, 0.8D, true, true);
        }
        if (message.contains("订单") || message.contains("单子")) {
            return new AiIntentResult(AiScenarioType.ORDER_ADVISOR, "LIST_ORDERS", slots, 0.7D, true, true);
        }
        return new AiIntentResult(AiScenarioType.ORDER_ADVISOR, "NONE", slots, 0.3D, true, false);
    }

    private AiIntentResult recognizeTravelContent(String message, AiScenarioType scenario) {
        Map<String, Object> slots = new LinkedHashMap<>();
        slots.put(AiIntentSlots.LIMIT, 10);
        if (scenario == AiScenarioType.GUIDE_QA || message.contains("攻略") || message.contains("避坑")) {
            String keyword = message.replace("攻略", "").replace("避坑", "").trim();
            putText(slots, AiIntentSlots.KEYWORD, keyword);
            putText(slots, AiIntentSlots.SPOT_NAME, keyword);
            return new AiIntentResult(AiScenarioType.GUIDE_QA, "GUIDE_SEARCH", slots, 0.8D, false, true);
        }
        if (message.contains("开放时间")) {
            putText(slots, AiIntentSlots.KEYWORD, normalizeTravelKeyword(message));
            putText(slots, AiIntentSlots.FACT_FIELD, "openTime");
            return new AiIntentResult(AiScenarioType.SPOT_QA, "SPOT_FACT", slots, 0.9D, false, true);
        }
        putText(slots, AiIntentSlots.KEYWORD, normalizeTravelKeyword(message));
        return new AiIntentResult(scenario, "SPOT_SEARCH", slots, 0.6D, false, true);
    }

    private AiIntentResult recognizeTravelPlan(String message) {
        Map<String, Object> slots = new LinkedHashMap<>();
        putInt(slots, AiIntentSlots.BUDGET, extractInt(BUDGET_PATTERN, message));
        putInt(slots, AiIntentSlots.DAYS, extractDays(message));
        putText(slots, AiIntentSlots.GROUP, message.contains("父母") ? "父母长辈" : "");
        putText(slots, AiIntentSlots.PREFERENCES, message.contains("轻松") ? "轻松" : "");
        slots.put(AiIntentSlots.LIMIT, 5);
        return new AiIntentResult(AiScenarioType.TRAVEL_PLANNER, "PLAN_TRIP", slots, 0.8D, false, true);
    }

    private AiIntentResult recognizeRecommendation(String message) {
        String intent = message.contains("相似") ? "SIMILAR_SPOTS" : "EXPLAIN_RECOMMENDATIONS";
        return new AiIntentResult(AiScenarioType.RECOMMENDATION_EXPLAINER, intent, new LinkedHashMap<>(), 0.8D, false, true);
    }

    private AiIntentResult recognizeProfile(String message) {
        String intent = (message.contains("偏好") || message.contains("画像")) ? "PREFERENCE_SUMMARY" : "NONE";
        return new AiIntentResult(AiScenarioType.USER_PROFILE_ANALYZER, intent, new LinkedHashMap<>(), 0.8D, true, !"NONE".equals(intent));
    }

    private AiIntentResult recognizeOperation(String message) {
        Map<String, Object> slots = new LinkedHashMap<>();
        if (message.contains("热门景点")) {
            return new AiIntentResult(AiScenarioType.OPERATION_ANALYZER, "HOT_SPOTS", slots, 0.8D, true, true);
        }
        int days = extractDays(message);
        if (days <= 0) {
            days = extractTrendDays(message);
        }
        if (days > 0) {
            slots.put(AiIntentSlots.DAYS, days);
            return new AiIntentResult(AiScenarioType.OPERATION_ANALYZER, "ORDER_TREND", slots, 0.8D, true, true);
        }
        return new AiIntentResult(AiScenarioType.OPERATION_ANALYZER, "NONE", slots, 0.2D, true, false);
    }

    private String buildSystemPrompt(AiScenarioType scenario) {
        return """
                你是 WayTrip 的意图识别器。
                你只输出一段 JSON，不要输出解释。
                根据当前场景识别用户真实意图和可提取的槽位。
                当前场景：%s
                可用槽位：intent, orderNo, status, keyword, spotName, city, factField, budget, days, group, preferences, spotId, limit
                """.formatted(scenario.name());
    }

    private boolean requiresLogin(AiScenarioType scenario) {
        return scenario == AiScenarioType.ORDER_ADVISOR
                || scenario == AiScenarioType.USER_PROFILE_ANALYZER
                || scenario == AiScenarioType.OPERATION_ANALYZER;
    }

    private boolean requiresTool(AiScenarioType scenario) {
        return scenario != AiScenarioType.CUSTOMER_SERVICE;
    }

    private String extractOrderNo(String message) {
        Matcher matcher = ORDER_NO_PATTERN.matcher(message);
        return matcher.find() ? matcher.group() : "";
    }

    private int extractInt(Pattern pattern, String message) {
        Matcher matcher = pattern.matcher(message);
        return matcher.find() ? Integer.parseInt(matcher.group(1)) : 0;
    }

    private int extractDays(String message) {
        Matcher matcher = DAY_PATTERN.matcher(message);
        if (!matcher.find()) {
            return 0;
        }
        if (StringUtils.hasText(matcher.group(1))) {
            return Integer.parseInt(matcher.group(1));
        }
        return 2;
    }

    private int extractTrendDays(String message) {
        Matcher matcher = Pattern.compile("(\\d+)\\s*天").matcher(message);
        return matcher.find() ? Integer.parseInt(matcher.group(1)) : 0;
    }

    private void putText(Map<String, Object> slots, String key, String value) {
        if (StringUtils.hasText(value)) {
            slots.put(key, value.trim());
        }
    }

    private void putInt(Map<String, Object> slots, String key, int value) {
        if (value > 0) {
            slots.put(key, value);
        }
    }

    private String normalizeKeyword(String keyword, AiScenarioType scenario) {
        if (!StringUtils.hasText(keyword)) {
            return "";
        }
        return (scenario == AiScenarioType.SPOT_QA || scenario == AiScenarioType.GUIDE_QA)
                ? normalizeTravelKeyword(keyword)
                : keyword.trim();
    }

    /**
     * 统一清洗景点与攻略关键词，避免无意义问法直接进入工具参数。
     *
     * @param rawKeyword 原始关键词
     * @return 清洗后的关键词
     */
    private String normalizeTravelKeyword(String rawKeyword) {
        if (!StringUtils.hasText(rawKeyword)) {
            return "";
        }
        String normalized = rawKeyword.trim()
                .replace("开放时间", "")
                .replace("门票", "")
                .replace("票价", "")
                .replace("价格", "")
                .replace("多少钱", "")
                .replace("地址", "")
                .replace("在哪", "")
                .replace("怎么去", "")
                .replace("好玩吗", "")
                .replace("值得去吗", "")
                .replace("攻略", "")
                .replace("避坑", "")
                .replace("介绍", "")
                .trim();
        return normalized.replaceAll("\\s+", " ");
    }
}

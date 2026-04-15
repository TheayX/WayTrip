package com.travel.service.ai.chat.planner;

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
 * 行程规划 AI 意图分类器，负责把自然语言规划需求转成结构化槽位。
 */
@Service
@RequiredArgsConstructor
public class TravelPlanIntentClassifier {

    private static final String SYSTEM_PROMPT = """
            你是 WayTrip 行程规划意图分类器。你的任务是提取行程规划需求，只输出 JSON。
            不要回答用户问题，不要解释，不要输出 Markdown。
            可选 intent：
            - PLAN_TRIP：规划路线、行程安排、预算玩法、周末游、一日游、多日游
            - NONE：不属于行程规划
            输出 JSON 字段：
            {"intent":"...","budget":null,"days":null,"group":"","preferences":"","limit":5}
            budget 输出数字或 null；days 输出 1 到 7 的数字或 null；group 输出亲子、情侣、父母长辈、朋友或空字符串。
            preferences 输出用户偏好关键词，例如夜景、拍照、自然、历史、低预算、轻松。
            """;

    private final AiJsonIntentClassificationSupport intentClassificationSupport;
    private final TravelPlanIntentResolver fallbackResolver;

    /**
     * 分类行程规划意图。
     *
     * @param userMessage 用户问题
     * @return 全局意图包
     */
    public AiIntentClassificationResult classify(String userMessage) {
        AiIntentClassificationResult fallback = fallbackResolver.resolve(userMessage);
        return intentClassificationSupport.classify(
                "行程规划 AI",
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
        TravelPlanIntent intent = parseIntent(root.path("intent").asText(""));
        Integer budget = root.path("budget").isNumber() ? root.path("budget").asInt() : null;
        Integer days = root.path("days").isNumber() ? root.path("days").asInt() : null;
        String group = root.path("group").asText("");
        String preferences = root.path("preferences").asText("");
        int limit = normalizeLimit(root.path("limit").asInt(5));
        return toIntentPackage(intent, budget, days, group, preferences, limit);
    }

    private AiIntentClassificationResult normalizeClassifiedResult(AiIntentClassificationResult classified,
                                                                  AiIntentClassificationResult fallback) {
        TravelPlanIntent intent = parseIntent(classified.intent());
        if (intent == TravelPlanIntent.NONE) {
            return TravelPlanIntent.NONE.name().equals(fallback.intent()) ? classified : fallback;
        }
        Integer budget = classified.slotAsInt(AiIntentSlots.BUDGET, fallback.slotAsInt(AiIntentSlots.BUDGET, 0));
        Integer days = classified.slotAsInt(AiIntentSlots.DAYS, fallback.slotAsInt(AiIntentSlots.DAYS, 1));
        String group = StringUtils.hasText(classified.slotAsString(AiIntentSlots.GROUP))
                ? classified.slotAsString(AiIntentSlots.GROUP)
                : fallback.slotAsString(AiIntentSlots.GROUP);
        String preferences = StringUtils.hasText(classified.slotAsString(AiIntentSlots.PREFERENCES))
                ? classified.slotAsString(AiIntentSlots.PREFERENCES)
                : fallback.slotAsString(AiIntentSlots.PREFERENCES);
        return toIntentPackage(intent, budget, days, group, preferences, classified.slotAsInt(AiIntentSlots.LIMIT, 5));
    }

    private AiIntentClassificationResult toIntentPackage(TravelPlanIntent intent,
                                                        Integer budget,
                                                        Integer days,
                                                        String group,
                                                        String preferences,
                                                        int limit) {
        Map<String, Object> slots = new LinkedHashMap<>();
        if (budget != null && budget > 0) {
            slots.put(AiIntentSlots.BUDGET, budget);
        }
        if (days != null && days > 0) {
            slots.put(AiIntentSlots.DAYS, Math.min(days, 7));
        }
        if (StringUtils.hasText(group)) {
            slots.put(AiIntentSlots.GROUP, group.trim());
        }
        if (StringUtils.hasText(preferences)) {
            slots.put(AiIntentSlots.PREFERENCES, preferences.trim());
            slots.put(AiIntentSlots.KEYWORD, preferences.trim());
        }
        slots.put(AiIntentSlots.LIMIT, normalizeLimit(limit));
        return new AiIntentClassificationResult(
                AiScenarioType.TRAVEL_PLANNER,
                intent.name(),
                slots,
                intent == TravelPlanIntent.NONE ? 0D : 0.78D,
                false,
                intent != TravelPlanIntent.NONE
        );
    }

    private TravelPlanIntent parseIntent(String value) {
        if (!StringUtils.hasText(value)) {
            return TravelPlanIntent.NONE;
        }
        try {
            return TravelPlanIntent.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return TravelPlanIntent.NONE;
        }
    }

    private int normalizeLimit(int value) {
        if (value <= 0) {
            return 5;
        }
        return Math.min(value, 10);
    }
}

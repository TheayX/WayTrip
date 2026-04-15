package com.travel.service.ai.chat.intent;

import com.travel.enums.ai.AiScenarioType;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.Objects;

/**
 * AI 全局意图包，统一承载场景、模块意图、槽位和执行要求。
 *
 * @param scenario 全局场景
 * @param intent 模块内意图
 * @param slots 槽位集合
 * @param confidence 置信度
 * @param requiresLogin 是否需要登录
 * @param requiresTool 是否需要工具
 */
public record AiIntentClassificationResult(
        AiScenarioType scenario,
        String intent,
        Map<String, Object> slots,
        double confidence,
        boolean requiresLogin,
        boolean requiresTool
) {

    public static AiIntentClassificationResult of(AiScenarioType scenario, String intent) {
        return new AiIntentClassificationResult(scenario, intent, Map.of(), 0D, false, false);
    }

    public String slotAsString(String key) {
        Object value = slots == null ? null : slots.get(key);
        return value == null ? "" : Objects.toString(value, "").trim();
    }

    public int slotAsInt(String key, int fallback) {
        Object value = slots == null ? null : slots.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (!StringUtils.hasText(Objects.toString(value, ""))) {
            return fallback;
        }
        try {
            return Integer.parseInt(Objects.toString(value));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    public long slotAsLong(String key, long fallback) {
        Object value = slots == null ? null : slots.get(key);
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (!StringUtils.hasText(Objects.toString(value, ""))) {
            return fallback;
        }
        try {
            return Long.parseLong(Objects.toString(value));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}

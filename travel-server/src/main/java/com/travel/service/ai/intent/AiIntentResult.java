package com.travel.service.ai.intent;

import com.travel.enums.ai.AiScenarioType;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * AI 意图识别结果。
 */
public record AiIntentResult(
        AiScenarioType scenario,
        String intent,
        Map<String, Object> slots,
        Double confidence,
        boolean requiresLogin,
        boolean requiresTool
) {

    /**
     * 返回字符串槽位。
     *
     * @param slot 槽位名
     * @return 槽位值
     */
    public String slotAsString(String slot) {
        Object value = slots == null ? null : slots.get(slot);
        return value == null ? "" : String.valueOf(value).trim();
    }

    /**
     * 返回 int 槽位。
     *
     * @param slot 槽位名
     * @param fallback 默认值
     * @return int 值
     */
    public int slotAsInt(String slot, int fallback) {
        Object value = slots == null ? null : slots.get(slot);
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String stringValue) {
            try {
                return Integer.parseInt(stringValue.trim());
            } catch (NumberFormatException ignored) {
            }
        }
        return fallback;
    }

    /**
     * 返回 long 槽位。
     *
     * @param slot 槽位名
     * @param fallback 默认值
     * @return long 值
     */
    public long slotAsLong(String slot, long fallback) {
        Object value = slots == null ? null : slots.get(slot);
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String stringValue) {
            try {
                return Long.parseLong(stringValue.trim());
            } catch (NumberFormatException ignored) {
            }
        }
        return fallback;
    }

    /**
     * 构建一个空槽位结果。
     *
     * @param scenario 场景
     * @param intent 意图名
     * @return 识别结果
     */
    public static AiIntentResult of(AiScenarioType scenario, String intent) {
        return new AiIntentResult(scenario, intent, new LinkedHashMap<>(), 0.6D, false, false);
    }
}

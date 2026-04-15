package com.travel.service.ai.chat.operation;

import com.travel.enums.ai.AiScenarioType;
import com.travel.service.ai.chat.intent.AiIntentClassificationResult;
import com.travel.service.ai.chat.intent.AiIntentSlots;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 运营分析意图规则解析器，负责识别后台统计类问题。
 */
@Component
public class OperationAiIntentResolver {

    private static final Pattern DAYS_PATTERN = Pattern.compile("最近\\s*(\\d{1,2})\\s*天");

    /**
     * 解析运营分析意图。
     *
     * @param userMessage 用户问题
     * @return 意图包
     */
    public AiIntentClassificationResult resolve(String userMessage) {
        if (!StringUtils.hasText(userMessage)) {
            return toIntentPackage(OperationAiIntent.NONE, 7, 5);
        }
        if (containsAny(userMessage, "热门景点", "景点热度", "热度最高", "哪些景点热门")) {
            return toIntentPackage(OperationAiIntent.HOT_SPOTS, extractDays(userMessage), 5);
        }
        if (containsAny(userMessage, "订单趋势", "订单变化", "营收趋势", "最近订单", "趋势")) {
            return toIntentPackage(OperationAiIntent.ORDER_TREND, extractDays(userMessage), 5);
        }
        if (containsAny(userMessage, "运营", "概览", "大盘", "总览", "后台数据", "经营情况")) {
            return toIntentPackage(OperationAiIntent.OPERATION_OVERVIEW, extractDays(userMessage), 5);
        }
        return toIntentPackage(OperationAiIntent.NONE, 7, 5);
    }

    private AiIntentClassificationResult toIntentPackage(OperationAiIntent intent, int days, int limit) {
        Map<String, Object> slots = new LinkedHashMap<>();
        slots.put(AiIntentSlots.DAYS, Math.min(Math.max(days, 1), 30));
        slots.put(AiIntentSlots.LIMIT, Math.min(Math.max(limit, 1), 10));
        return new AiIntentClassificationResult(
                AiScenarioType.OPERATION_ANALYZER,
                intent.name(),
                slots,
                intent == OperationAiIntent.NONE ? 0D : 0.7D,
                true,
                intent != OperationAiIntent.NONE
        );
    }

    private int extractDays(String userMessage) {
        Matcher matcher = DAYS_PATTERN.matcher(userMessage);
        if (!matcher.find()) {
            return 7;
        }
        try {
            return Integer.parseInt(matcher.group(1));
        } catch (NumberFormatException e) {
            return 7;
        }
    }

    private boolean containsAny(String source, String... keywords) {
        for (String keyword : keywords) {
            if (source.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}

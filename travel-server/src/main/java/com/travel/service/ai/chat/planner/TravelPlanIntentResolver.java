package com.travel.service.ai.chat.planner;

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
 * 行程规划规则兜底，负责补齐预算、天数和人群等稳定槽位。
 */
@Component
public class TravelPlanIntentResolver {

    private static final Pattern BUDGET_PATTERN = Pattern.compile("(预算)?\\s*(\\d{2,6})\\s*(元|块|rmb|RMB)?");
    private static final Pattern DAYS_PATTERN = Pattern.compile("(\\d+)\\s*(天|日)");

    /**
     * 解析行程规划兜底意图。
     *
     * @param userMessage 用户问题
     * @return 全局意图包
     */
    public AiIntentClassificationResult resolve(String userMessage) {
        if (!StringUtils.hasText(userMessage)) {
            return toIntentPackage(TravelPlanIntent.NONE, null, null, "", "");
        }
        if (!containsAny(userMessage, "行程", "路线", "规划", "安排", "怎么玩", "去哪玩", "一日游", "两日游", "三日游", "周末")) {
            return toIntentPackage(TravelPlanIntent.NONE, null, null, "", "");
        }
        return toIntentPackage(
                TravelPlanIntent.PLAN_TRIP,
                extractBudget(userMessage),
                extractDays(userMessage),
                extractGroup(userMessage),
                extractPreferences(userMessage)
        );
    }

    private AiIntentClassificationResult toIntentPackage(TravelPlanIntent intent,
                                                        Integer budget,
                                                        Integer days,
                                                        String group,
                                                        String preferences) {
        Map<String, Object> slots = new LinkedHashMap<>();
        if (budget != null && budget > 0) {
            slots.put(AiIntentSlots.BUDGET, budget);
        }
        if (days != null && days > 0) {
            slots.put(AiIntentSlots.DAYS, Math.min(days, 7));
        }
        if (StringUtils.hasText(group)) {
            slots.put(AiIntentSlots.GROUP, group);
        }
        if (StringUtils.hasText(preferences)) {
            slots.put(AiIntentSlots.PREFERENCES, preferences);
            slots.put(AiIntentSlots.KEYWORD, preferences);
        }
        slots.put(AiIntentSlots.LIMIT, 5);
        return new AiIntentClassificationResult(
                AiScenarioType.TRAVEL_PLANNER,
                intent.name(),
                slots,
                intent == TravelPlanIntent.NONE ? 0D : 0.68D,
                false,
                intent != TravelPlanIntent.NONE
        );
    }

    private Integer extractBudget(String userMessage) {
        Matcher matcher = BUDGET_PATTERN.matcher(userMessage);
        if (!matcher.find()) {
            return null;
        }
        try {
            return Integer.parseInt(matcher.group(2));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer extractDays(String userMessage) {
        if (userMessage.contains("周末")) {
            return 2;
        }
        if (userMessage.contains("一日游")) {
            return 1;
        }
        if (userMessage.contains("两日游")) {
            return 2;
        }
        if (userMessage.contains("三日游")) {
            return 3;
        }
        Matcher matcher = DAYS_PATTERN.matcher(userMessage);
        if (!matcher.find()) {
            return null;
        }
        try {
            return Integer.parseInt(matcher.group(1));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String extractGroup(String userMessage) {
        if (containsAny(userMessage, "父母", "老人", "长辈")) {
            return "父母长辈";
        }
        if (containsAny(userMessage, "情侣", "对象", "约会")) {
            return "情侣";
        }
        if (containsAny(userMessage, "亲子", "孩子", "小孩")) {
            return "亲子";
        }
        if (containsAny(userMessage, "朋友", "同学")) {
            return "朋友";
        }
        return "";
    }

    private String extractPreferences(String userMessage) {
        StringBuilder preferences = new StringBuilder();
        appendIfContains(preferences, userMessage, "夜景");
        appendIfContains(preferences, userMessage, "拍照");
        appendIfContains(preferences, userMessage, "自然");
        appendIfContains(preferences, userMessage, "历史");
        appendIfContains(preferences, userMessage, "博物馆");
        appendIfContains(preferences, userMessage, "低预算");
        appendIfContains(preferences, userMessage, "轻松");
        return preferences.toString();
    }

    private void appendIfContains(StringBuilder builder, String source, String keyword) {
        if (!source.contains(keyword)) {
            return;
        }
        if (!builder.isEmpty()) {
            builder.append(' ');
        }
        builder.append(keyword);
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

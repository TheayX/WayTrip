package com.travel.service.ai.chat.recommendation;

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
 * 推荐解释规则兜底，负责模型失败时补齐推荐意图和景点 ID。
 */
@Component
public class RecommendationExplainIntentResolver {

    private static final Pattern SPOT_ID_PATTERN = Pattern.compile("(景点)?\\s*(\\d{1,12})");

    /**
     * 解析推荐解释兜底意图。
     *
     * @param userMessage 用户问题
     * @return 全局意图包
     */
    public AiIntentClassificationResult resolve(String userMessage) {
        if (!StringUtils.hasText(userMessage)) {
            return toIntentPackage(RecommendationExplainIntent.NONE, null);
        }
        if (containsAny(userMessage, "相似", "类似")) {
            return toIntentPackage(RecommendationExplainIntent.SIMILAR_SPOTS, extractSpotId(userMessage));
        }
        if (containsAny(userMessage, "热门", "大家都在看", "推荐几个")) {
            return toIntentPackage(RecommendationExplainIntent.HOT_RECOMMENDATIONS, null);
        }
        if (containsAny(userMessage, "为什么推荐", "推荐理由", "为什么给我推荐", "适合我吗", "推荐")) {
            return toIntentPackage(RecommendationExplainIntent.EXPLAIN_RECOMMENDATIONS, extractSpotId(userMessage));
        }
        return toIntentPackage(RecommendationExplainIntent.NONE, null);
    }

    private AiIntentClassificationResult toIntentPackage(RecommendationExplainIntent intent, Long spotId) {
        Map<String, Object> slots = new LinkedHashMap<>();
        if (spotId != null && spotId > 0) {
            slots.put(AiIntentSlots.SPOT_ID, spotId);
        }
        slots.put(AiIntentSlots.LIMIT, 5);
        return new AiIntentClassificationResult(
                AiScenarioType.RECOMMENDATION_EXPLAINER,
                intent.name(),
                slots,
                intent == RecommendationExplainIntent.NONE ? 0D : 0.68D,
                intent == RecommendationExplainIntent.EXPLAIN_RECOMMENDATIONS,
                intent != RecommendationExplainIntent.NONE
        );
    }

    private Long extractSpotId(String userMessage) {
        Matcher matcher = SPOT_ID_PATTERN.matcher(userMessage);
        if (!matcher.find()) {
            return null;
        }
        try {
            return Long.parseLong(matcher.group(2));
        } catch (NumberFormatException e) {
            return null;
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

package com.travel.service.ai.chat.planner;

import com.travel.common.exception.BusinessException;
import com.travel.service.ai.chat.intent.AiIntentClassificationResult;
import com.travel.service.ai.chat.intent.AiIntentSlots;
import com.travel.service.ai.tool.RecommendationAiTools;
import com.travel.service.ai.tool.SpotAiTools;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

/**
 * 行程规划工具策略，组合推荐景点和攻略摘要。
 */
@Component
@RequiredArgsConstructor
public class TravelPlanToolPolicy {

    private final RecommendationAiTools recommendationAiTools;
    private final SpotAiTools spotAiTools;

    /**
     * 执行行程规划工具策略。
     *
     * @param intent 意图类型
     * @param intentResult 全局意图包
     * @return 工具执行结果
     */
    public TravelPlanToolResult execute(TravelPlanIntent intent, AiIntentClassificationResult intentResult) {
        if (intent == TravelPlanIntent.NONE) {
            return new TravelPlanToolResult(intent, intentResult, List.of(), List.of(), "");
        }
        int limit = intentResult.slotAsInt(AiIntentSlots.LIMIT, 5);
        Map<String, Object> recommendationPayload = fetchRecommendations(limit);
        List<Map<String, Object>> spots = extractSpotList(recommendationPayload);
        String recommendationType = String.valueOf(recommendationPayload.getOrDefault("type", "hot"));
        List<Map<String, Object>> guides = fetchGuides(intentResult, limit);
        return new TravelPlanToolResult(intent, intentResult, spots, guides, recommendationType);
    }

    private Map<String, Object> fetchRecommendations(int limit) {
        try {
            return recommendationAiTools.getPersonalizedRecommendations(limit);
        } catch (BusinessException e) {
            return recommendationAiTools.getHotSpotRecommendations(limit);
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractSpotList(Map<String, Object> payload) {
        Object personalizedSpots = payload.get("spots");
        if (personalizedSpots instanceof List<?> list) {
            return (List<Map<String, Object>>) list;
        }
        return List.of();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> fetchGuides(AiIntentClassificationResult intentResult, int limit) {
        String keyword = intentResult.slotAsString(AiIntentSlots.PREFERENCES);
        if (!StringUtils.hasText(keyword)) {
            keyword = intentResult.slotAsString(AiIntentSlots.GROUP);
        }
        if (!StringUtils.hasText(keyword)) {
            return List.of();
        }
        Map<String, Object> payload = spotAiTools.getGuideSummariesByKeyword(keyword, Math.min(limit, 5));
        Object listValue = payload.get("list");
        return listValue instanceof List<?> list ? (List<Map<String, Object>>) list : List.of();
    }
}

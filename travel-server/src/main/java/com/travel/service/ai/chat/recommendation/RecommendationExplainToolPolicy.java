package com.travel.service.ai.chat.recommendation;

import com.travel.common.exception.BusinessException;
import com.travel.service.ai.chat.intent.AiIntentClassificationResult;
import com.travel.service.ai.chat.intent.AiIntentSlots;
import com.travel.service.ai.tool.RecommendationAiTools;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 推荐解释工具策略，根据推荐意图确定性调用推荐工具。
 */
@Component
@RequiredArgsConstructor
public class RecommendationExplainToolPolicy {

    private final RecommendationAiTools recommendationAiTools;

    /**
     * 执行推荐解释工具策略。
     *
     * @param intent 意图类型
     * @param intentResult 全局意图包
     * @return 工具执行结果
     */
    public RecommendationExplainToolResult execute(RecommendationExplainIntent intent, AiIntentClassificationResult intentResult) {
        int limit = intentResult.slotAsInt(AiIntentSlots.LIMIT, 5);
        Map<String, Object> payload = switch (intent) {
            case EXPLAIN_RECOMMENDATIONS -> fetchPersonalizedOrHot(limit);
            case HOT_RECOMMENDATIONS -> recommendationAiTools.getHotSpotRecommendations(limit);
            case SIMILAR_SPOTS -> fetchSimilarSpots(intentResult, limit);
            default -> Map.of();
        };
        return new RecommendationExplainToolResult(intent, intentResult, payload);
    }

    private Map<String, Object> fetchPersonalizedOrHot(int limit) {
        try {
            return recommendationAiTools.getPersonalizedRecommendations(limit);
        } catch (BusinessException e) {
            Map<String, Object> hotPayload = recommendationAiTools.getHotSpotRecommendations(limit);
            return Map.of(
                    "type", "hot",
                    "needPreference", false,
                    "spots", hotPayload.getOrDefault("spots", java.util.List.of())
            );
        }
    }

    private Map<String, Object> fetchSimilarSpots(AiIntentClassificationResult intentResult, int limit) {
        long spotId = intentResult.slotAsLong(AiIntentSlots.SPOT_ID, 0L);
        if (spotId <= 0) {
            return Map.of("missingSpotId", true);
        }
        return recommendationAiTools.getSimilarSpots(spotId, limit);
    }
}

package com.travel.service.ai.chat.recommendation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travel.service.ai.chat.intent.AiIntentClassificationResult;
import com.travel.service.ai.chat.intent.AiIntentSlots;
import com.travel.service.ai.chat.intent.AiJsonIntentClassificationSupport;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 推荐解释意图分类器测试，覆盖模型 JSON 输出解析。
 */
class RecommendationExplainIntentClassifierTest {

    private final RecommendationExplainIntentClassifier classifier = new RecommendationExplainIntentClassifier(
            new AiJsonIntentClassificationSupport(null, new ObjectMapper()),
            new RecommendationExplainIntentResolver()
    );

    @Test
    void parseSimilarSpotsModelReply() throws Exception {
        AiIntentClassificationResult result = classifier.parseModelReply("""
                {"intent":"SIMILAR_SPOTS","spotId":123,"limit":8}
                """);

        assertEquals(RecommendationExplainIntent.SIMILAR_SPOTS.name(), result.intent());
        assertEquals(123L, result.slotAsLong(AiIntentSlots.SPOT_ID, 0L));
        assertEquals(8, result.slotAsInt(AiIntentSlots.LIMIT, 0));
    }

    @Test
    void fallbackRecognizesRecommendationReason() {
        AiIntentClassificationResult result = new RecommendationExplainIntentResolver().resolve("为什么推荐这些景点");

        assertEquals(RecommendationExplainIntent.EXPLAIN_RECOMMENDATIONS.name(), result.intent());
    }
}

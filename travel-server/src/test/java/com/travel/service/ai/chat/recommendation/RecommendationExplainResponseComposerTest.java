package com.travel.service.ai.chat.recommendation;

import com.travel.enums.ai.AiScenarioType;
import com.travel.service.ai.chat.intent.AiIntentClassificationResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 推荐解释回复生成器测试，固定推荐理由和相似景点输出。
 */
class RecommendationExplainResponseComposerTest {

    private final RecommendationExplainResponseComposer composer = new RecommendationExplainResponseComposer();

    @Test
    void composeRecommendationExplanation() {
        String reply = composer.compose(new RecommendationExplainToolResult(
                RecommendationExplainIntent.EXPLAIN_RECOMMENDATIONS,
                new AiIntentClassificationResult(AiScenarioType.RECOMMENDATION_EXPLAINER, "EXPLAIN_RECOMMENDATIONS", Map.of(), 0.8D, true, true),
                Map.of(
                        "type", "hot",
                        "spots", List.of(Map.of(
                                "name", "西湖",
                                "categoryName", "自然风光",
                                "regionName", "杭州",
                                "avgRating", "4.8"
                        ))
                )
        ));

        assertTrue(reply.contains("平台热门趋势"));
        assertTrue(reply.contains("西湖"));
        assertTrue(reply.contains("不代表你一定会喜欢"));
    }

    @Test
    void composeSimilarSpotsRequiresSpotId() {
        String reply = composer.compose(new RecommendationExplainToolResult(
                RecommendationExplainIntent.SIMILAR_SPOTS,
                new AiIntentClassificationResult(AiScenarioType.RECOMMENDATION_EXPLAINER, "SIMILAR_SPOTS", Map.of(), 0.8D, false, true),
                Map.of("missingSpotId", true)
        ));

        assertTrue(reply.contains("需要先明确具体景点 ID"));
    }
}

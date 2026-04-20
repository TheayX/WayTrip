package com.travel.service.ai.chat;

import com.travel.enums.ai.AiScenarioType;
import com.travel.service.ai.intent.AiScenarioFallback;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * AI 场景路由测试。
 */
class AiScenarioRouterTest {

    private final AiScenarioRouter router = new AiScenarioRouter(new AiScenarioFallback());

    @Test
    void routeUsesExactScenarioHintFirst() {
        assertEquals(
                AiScenarioType.TRAVEL_PLANNER,
                router.route("看看我适合哪些景点", "TRAVEL_PLANNER", "Home")
        );
    }

    @Test
    void routeRecognizesSuitabilityQuestionAsPlanner() {
        assertEquals(
                AiScenarioType.TRAVEL_PLANNER,
                router.route("看看我适合哪些景点", "", "Home")
        );
    }

    @Test
    void routeUsesRecommendationPageWhenNoHintProvided() {
        assertEquals(
                AiScenarioType.RECOMMENDATION_EXPLAINER,
                router.route("这些结果为什么推荐给我", "", "Recommendations")
        );
    }

    @Test
    void routeUsesPlannerHintForTravelAdviceQuestion() {
        assertEquals(
                AiScenarioType.TRAVEL_PLANNER,
                router.route("最近想带爸妈轻松玩两天，有什么建议", "TRAVEL_PLANNER", "Home")
        );
    }

    @Test
    void routeUsesRecommendationPageForRecommendationExplanation() {
        assertEquals(
                AiScenarioType.RECOMMENDATION_EXPLAINER,
                router.route("为什么推荐这个景点给我", "", "Recommendations")
        );
    }
}

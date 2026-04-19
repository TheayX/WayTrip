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
}

package com.travel.service.ai.chat.planner;

import com.travel.enums.ai.AiScenarioType;
import com.travel.service.ai.chat.intent.AiIntentClassificationResult;
import com.travel.service.ai.chat.intent.AiIntentSlots;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 行程规划回复生成器测试，固定结构化路线输出。
 */
class TravelPlanResponseComposerTest {

    private final TravelPlanResponseComposer composer = new TravelPlanResponseComposer();

    @Test
    void composePlanWithSpotsAndGuides() {
        AiIntentClassificationResult intent = new AiIntentClassificationResult(
                AiScenarioType.TRAVEL_PLANNER,
                TravelPlanIntent.PLAN_TRIP.name(),
                Map.of(
                        AiIntentSlots.BUDGET, 500,
                        AiIntentSlots.DAYS, 2,
                        AiIntentSlots.GROUP, "情侣",
                        AiIntentSlots.PREFERENCES, "夜景 拍照"
                ),
                0.8D,
                false,
                true
        );
        String reply = composer.compose(new TravelPlanToolResult(
                TravelPlanIntent.PLAN_TRIP,
                intent,
                List.of(
                        Map.of("name", "西湖", "categoryName", "自然风光", "regionName", "杭州", "avgRating", "4.8"),
                        Map.of("name", "外滩", "categoryName", "夜景", "regionName", "上海", "avgRating", "4.7")
                ),
                List.of(Map.of("title", "夜景拍照攻略", "category", "摄影")),
                "hot"
        ));

        assertTrue(reply.contains("保守行程"));
        assertTrue(reply.contains("第 1 天"));
        assertTrue(reply.contains("西湖"));
        assertTrue(reply.contains("可参考攻略"));
        assertTrue(reply.contains("以详情页及实际出行为准"));
    }
}

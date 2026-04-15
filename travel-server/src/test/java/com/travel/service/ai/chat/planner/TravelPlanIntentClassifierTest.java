package com.travel.service.ai.chat.planner;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travel.service.ai.chat.intent.AiIntentClassificationResult;
import com.travel.service.ai.chat.intent.AiIntentSlots;
import com.travel.service.ai.chat.intent.AiJsonIntentClassificationSupport;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 行程规划意图分类器测试，覆盖模型 JSON 输出解析。
 */
class TravelPlanIntentClassifierTest {

    private final TravelPlanIntentClassifier classifier = new TravelPlanIntentClassifier(
            new AiJsonIntentClassificationSupport(null, new ObjectMapper()),
            new TravelPlanIntentResolver()
    );

    @Test
    void parseTripPlanModelReply() throws Exception {
        AiIntentClassificationResult result = classifier.parseModelReply("""
                {"intent":"PLAN_TRIP","budget":500,"days":2,"group":"父母长辈","preferences":"轻松 自然","limit":5}
                """);

        assertEquals(TravelPlanIntent.PLAN_TRIP.name(), result.intent());
        assertEquals(500, result.slotAsInt(AiIntentSlots.BUDGET, 0));
        assertEquals(2, result.slotAsInt(AiIntentSlots.DAYS, 0));
        assertEquals("父母长辈", result.slotAsString(AiIntentSlots.GROUP));
    }

    @Test
    void fallbackExtractsBudgetDaysAndGroup() {
        AiIntentClassificationResult result = new TravelPlanIntentResolver().resolve("预算500，带父母周末两日游怎么安排，轻松一点");

        assertEquals(TravelPlanIntent.PLAN_TRIP.name(), result.intent());
        assertEquals(500, result.slotAsInt(AiIntentSlots.BUDGET, 0));
        assertEquals(2, result.slotAsInt(AiIntentSlots.DAYS, 0));
        assertEquals("父母长辈", result.slotAsString(AiIntentSlots.GROUP));
    }
}

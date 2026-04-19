package com.travel.service.ai.intent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travel.enums.ai.AiScenarioType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 主链路意图识别服务测试，只覆盖当前运行时保留的核心行为。
 */
class AiIntentServiceTest {

    private final AiIntentService service = new AiIntentService(null, new ObjectMapper());

    @Test
    void recognizeTravelPlannerQuestion() {
        AiIntentResult result = service.recognize("预算500，带父母周末两日游怎么安排，轻松一点", AiScenarioType.TRAVEL_PLANNER);

        assertEquals("PLAN_TRIP", result.intent());
        assertEquals(500, result.slotAsInt(AiIntentSlots.BUDGET, 0));
        assertEquals(2, result.slotAsInt(AiIntentSlots.DAYS, 0));
        assertEquals("父母长辈", result.slotAsString(AiIntentSlots.GROUP));
    }

    @Test
    void recognizeSpotFactQuestion() {
        AiIntentResult result = service.recognize("故宫开放时间", AiScenarioType.SPOT_QA);

        assertEquals("SPOT_FACT", result.intent());
        assertEquals("故宫", result.slotAsString(AiIntentSlots.KEYWORD));
        assertEquals("openTime", result.slotAsString(AiIntentSlots.FACT_FIELD));
    }

    @Test
    void recognizeOrderRefundQuestion() {
        AiIntentResult result = service.recognize("T202604121416166706这个订单能退款吗", AiScenarioType.ORDER_ADVISOR);

        assertEquals("REFUND_ELIGIBILITY", result.intent());
        assertEquals("T202604121416166706", result.slotAsString(AiIntentSlots.ORDER_NO));
        assertTrue(result.requiresLogin());
    }
}

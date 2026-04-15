package com.travel.service.ai.chat.operation;

import com.travel.enums.ai.AiScenarioType;
import com.travel.service.ai.chat.intent.AiIntentClassificationResult;
import com.travel.service.ai.chat.intent.AiIntentSlots;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 运营分析 AI 意图解析器测试，固定后台统计类问法。
 */
class OperationAiIntentResolverTest {

    private final OperationAiIntentResolver resolver = new OperationAiIntentResolver();

    @Test
    void resolveOrderTrendWithDaysSlot() {
        AiIntentClassificationResult result = resolver.resolve("最近 14 天订单趋势怎么样");

        assertEquals(AiScenarioType.OPERATION_ANALYZER, result.scenario());
        assertEquals(OperationAiIntent.ORDER_TREND.name(), result.intent());
        assertEquals(14, result.slotAsInt(AiIntentSlots.DAYS, 0));
        assertTrue(result.requiresLogin());
    }

    @Test
    void resolveHotSpots() {
        AiIntentClassificationResult result = resolver.resolve("当前热门景点有哪些");

        assertEquals(OperationAiIntent.HOT_SPOTS.name(), result.intent());
    }
}

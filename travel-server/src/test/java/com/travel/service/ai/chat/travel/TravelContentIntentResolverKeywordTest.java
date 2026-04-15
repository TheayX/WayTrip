package com.travel.service.ai.chat.travel;

import com.travel.enums.ai.AiScenarioType;
import com.travel.service.ai.chat.intent.AiIntentClassificationResult;
import com.travel.service.ai.chat.intent.AiIntentSlots;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 旅行内容规则兜底关键词测试，避免事实问法噪音影响搜索。
 */
class TravelContentIntentResolverKeywordTest {

    private final TravelContentIntentResolver resolver = new TravelContentIntentResolver();

    @Test
    void resolveSpotFactStripsOpenTimeNoise() {
        AiIntentClassificationResult result = resolver.resolve("故宫开放时间", AiScenarioType.SPOT_QA);

        assertEquals(TravelContentIntent.SPOT_FACT.name(), result.intent());
        assertEquals("故宫", result.slotAsString(AiIntentSlots.KEYWORD));
        assertEquals("openTime", result.slotAsString(AiIntentSlots.FACT_FIELD));
    }
}

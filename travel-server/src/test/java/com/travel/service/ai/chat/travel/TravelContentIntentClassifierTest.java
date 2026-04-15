package com.travel.service.ai.chat.travel;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travel.enums.ai.AiScenarioType;
import com.travel.service.ai.chat.intent.AiIntentClassificationResult;
import com.travel.service.ai.chat.intent.AiIntentSlots;
import com.travel.service.ai.chat.intent.AiJsonIntentClassificationSupport;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 旅行内容意图分类器测试，覆盖模型 JSON 输出解析。
 */
class TravelContentIntentClassifierTest {

    private final TravelContentIntentClassifier classifier = new TravelContentIntentClassifier(
            new AiJsonIntentClassificationSupport(null, new ObjectMapper()),
            new TravelContentIntentResolver()
    );

    @Test
    void parseSpotFactModelReply() throws Exception {
        AiIntentClassificationResult result = classifier.parseModelReply("""
                {"intent":"SPOT_FACT","keyword":"西湖","spotName":"西湖","city":"杭州","limit":5}
                """, AiScenarioType.SPOT_QA);

        assertEquals(TravelContentIntent.SPOT_FACT.name(), result.intent());
        assertEquals("西湖", result.slotAsString(AiIntentSlots.KEYWORD));
        assertEquals("杭州", result.slotAsString(AiIntentSlots.CITY));
    }

    @Test
    void parseSpotFactModelReplyStripsQuestionNoise() throws Exception {
        AiIntentClassificationResult result = classifier.parseModelReply("""
                {"intent":"SPOT_FACT","keyword":"故宫开放时间","spotName":"","city":"北京","limit":5}
                """, AiScenarioType.SPOT_QA);

        assertEquals(TravelContentIntent.SPOT_FACT.name(), result.intent());
        assertEquals("故宫", result.slotAsString(AiIntentSlots.KEYWORD));
    }

    @Test
    void parseGuideSearchModelReply() throws Exception {
        AiIntentClassificationResult result = classifier.parseModelReply("""
                ```json
                {"intent":"GUIDE_SEARCH","keyword":"黄山避坑","spotName":"黄山","city":"","limit":20}
                ```
                """, AiScenarioType.GUIDE_QA);

        assertEquals(AiScenarioType.GUIDE_QA, result.scenario());
        assertEquals(TravelContentIntent.GUIDE_SEARCH.name(), result.intent());
        assertEquals(10, result.slotAsInt(AiIntentSlots.LIMIT, 0));
    }
}

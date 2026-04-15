package com.travel.service.ai.chat.travel;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 旅行内容关键词规范化测试，固定景点事实问法的搜索输入。
 */
class TravelContentKeywordNormalizerTest {

    @Test
    void normalizeOpeningHoursQuestionWithFullSpotName() {
        assertEquals("故宫博物院", TravelContentKeywordNormalizer.normalizeSearchKeyword("故宫博物院开放时间"));
    }

    @Test
    void normalizeOpeningHoursQuestionWithShortSpotName() {
        assertEquals("故宫", TravelContentKeywordNormalizer.normalizeSearchKeyword("故宫开放时间"));
    }

    @Test
    void buildFallbackKeywordsIncludesMuseumSuffix() {
        assertTrue(TravelContentKeywordNormalizer.buildFallbackKeywords("故宫").contains("故宫博物院"));
    }
}

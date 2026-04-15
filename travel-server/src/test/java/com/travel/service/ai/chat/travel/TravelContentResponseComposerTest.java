package com.travel.service.ai.chat.travel;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 旅行内容回复生成器测试，固定景点和攻略事实型回复。
 */
class TravelContentResponseComposerTest {

    private final TravelContentResponseComposer composer = new TravelContentResponseComposer();

    @Test
    void composeSpotFactReply() {
        String reply = composer.compose(new TravelContentToolResult(
                TravelContentIntent.SPOT_FACT,
                Map.of(
                        "total", 1,
                        "list", List.of(Map.of(
                                "name", "西湖",
                                "price", "0.00",
                                "avgRating", "4.8",
                                "categoryName", "自然风光",
                                "regionName", "杭州"
                        ))
                )
        ));

        assertTrue(reply.contains("查询到较匹配的景点"));
        assertTrue(reply.contains("西湖"));
        assertTrue(reply.contains("价格、开放安排"));
    }

    @Test
    void composeGuideSearchReply() {
        String reply = composer.compose(new TravelContentToolResult(
                TravelContentIntent.GUIDE_SEARCH,
                Map.of(
                        "total", 1,
                        "list", List.of(Map.of(
                                "title", "黄山一日游避坑指南",
                                "category", "避坑",
                                "viewCount", 120
                        ))
                )
        ));

        assertTrue(reply.contains("找到 1 篇相关攻略"));
        assertTrue(reply.contains("黄山一日游避坑指南"));
    }
}

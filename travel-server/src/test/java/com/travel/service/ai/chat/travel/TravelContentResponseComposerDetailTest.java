package com.travel.service.ai.chat.travel;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 旅行内容回复生成器测试，确保景点事实问答使用详情字段。
 */
class TravelContentResponseComposerDetailTest {

    private final TravelContentResponseComposer composer = new TravelContentResponseComposer();

    @Test
    void composeSpotFactWithDetailFields() {
        String reply = composer.compose(new TravelContentToolResult(
                TravelContentIntent.SPOT_FACT,
                Map.of(
                        "total", 1,
                        "list", List.of(Map.of("id", 1L, "name", "西湖")),
                        "detail", Map.of(
                                "name", "西湖",
                                "price", "0",
                                "openTime", "全天开放",
                                "address", "杭州市西湖区",
                                "avgRating", "4.8",
                                "categoryName", "自然风光",
                                "regionName", "杭州"
                        )
                )
        ));

        assertTrue(reply.contains("开放时间：全天开放"));
        assertTrue(reply.contains("地址：杭州市西湖区"));
    }
}

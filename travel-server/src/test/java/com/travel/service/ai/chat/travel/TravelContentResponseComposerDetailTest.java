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

    @Test
    void composeOpenTimeOnlyWhenFactFieldIsOpenTime() {
        String reply = composer.compose(new TravelContentToolResult(
                TravelContentIntent.SPOT_FACT,
                Map.of(
                        "factField", "openTime",
                        "total", 1,
                        "list", List.of(Map.of("id", 1L, "name", "故宫博物院")),
                        "detail", Map.of(
                                "name", "故宫博物院",
                                "price", "60.00",
                                "openTime", "08:30-17:00（周一闭馆）",
                                "address", "北京市东城区景山前街4号",
                                "avgRating", "4.8"
                        )
                )
        ));

        assertTrue(reply.contains("故宫博物院开放时间：08:30-17:00（周一闭馆）"));
        org.junit.jupiter.api.Assertions.assertFalse(reply.contains("门票/价格"));
        org.junit.jupiter.api.Assertions.assertFalse(reply.contains("地址：北京市"));
    }
}

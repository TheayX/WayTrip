package com.travel.service.ai.rag;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 知识层级推断测试。
 */
class AiKnowledgeLayerSupportTest {

    @Test
    void inferStrategyLayerFromTripPlanningRule() {
        assertEquals(
                AiKnowledgeLayerSupport.STRATEGY,
                AiKnowledgeLayerSupport.inferLayer("旅游规划与推荐回答策略", "guide:trip-planning", "旅游规划,工具调用")
        );
    }

    @Test
    void inferBoundaryLayerFromPolicyDocument() {
        assertEquals(
                AiKnowledgeLayerSupport.BOUNDARY,
                AiKnowledgeLayerSupport.inferLayer("平台订单售后说明", "policy:order-after-sale", "订单,退款")
        );
    }

    @Test
    void inferFactLayerWhenNoStrategyOrBoundaryHintExists() {
        assertEquals(
                AiKnowledgeLayerSupport.FACT,
                AiKnowledgeLayerSupport.inferLayer("西湖景点基础信息", "spot:west-lake", "景点,开放时间")
        );
    }
}

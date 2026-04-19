package com.travel.service.ai.chat;

import com.travel.dto.home.item.RecentViewedSpotItem;
import com.travel.dto.home.response.HotSpotResponse;
import com.travel.dto.home.response.RecentViewedSpotResponse;
import com.travel.dto.order.response.OrderDetailResponse;
import com.travel.dto.order.response.OrderListResponse;
import com.travel.dto.recommendation.response.RecommendationResponse;
import com.travel.enums.ai.AiScenarioType;
import com.travel.service.OrderService;
import com.travel.service.RecommendationService;
import com.travel.service.ai.intent.AiIntentResult;
import com.travel.service.ai.intent.AiIntentSlots;
import com.travel.service.ai.rule.OrderBusinessRuleProvider;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 对话上下文组装服务测试。
 */
class AiConversationContextServiceTest {

    private final OrderService orderService = mock(OrderService.class);
    private final RecommendationService recommendationService = mock(RecommendationService.class);
    private final OrderBusinessRuleProvider orderBusinessRuleProvider = mock(OrderBusinessRuleProvider.class);
    private final AiConversationContextService service =
            new AiConversationContextService(orderService, recommendationService, orderBusinessRuleProvider);

    @Test
    void assembleOrderContextIncludesRecentOrderAndMatchedOrder() {
        when(orderBusinessRuleProvider.describeRules()).thenReturn(Map.of(
                "generalOverview", List.of("订单问题可先看最近订单。"),
                "refundPolicy", List.of("退款以订单页为准。"),
                "disclaimer", "退款金额以页面为准。"
        ));
        when(orderService.getOrderDetailByOrderNo(eq(1L), eq("T202604121416166706"))).thenReturn(buildOrderDetail());
        when(orderService.getUserOrders(eq(1L), any())).thenReturn(buildOrderList());

        AiIntentResult intentResult = new AiIntentResult(
                AiScenarioType.ORDER_ADVISOR,
                "REFUND_ELIGIBILITY",
                new LinkedHashMap<>(Map.of(AiIntentSlots.ORDER_NO, "T202604121416166706")),
                0.9D,
                true,
                true
        );

        AiConversationContext context = service.assemble(1L, AiScenarioType.ORDER_ADVISOR, intentResult);

        assertFalse(context.isEmpty());
        assertTrue(context.promptText().contains("命中订单详情"));
        assertTrue(context.promptText().contains("最近订单摘要"));
        assertTrue(context.promptText().contains("订单规则摘要"));
    }

    @Test
    void assemblePlannerContextIncludesRecommendationsAndHotSpots() {
        when(recommendationService.getRecommendations(eq(1L), eq(3))).thenReturn(buildRecommendations());
        when(recommendationService.getHotSpots(eq(3))).thenReturn(buildHotSpots());
        when(recommendationService.getRecentViewedSpots(eq(7), eq(3))).thenReturn(buildRecentViewed());

        AiConversationContext context = service.assemble(
                1L,
                AiScenarioType.TRAVEL_PLANNER,
                AiIntentResult.of(AiScenarioType.TRAVEL_PLANNER, "PLAN_TRIP")
        );

        assertFalse(context.isEmpty());
        assertTrue(context.promptText().contains("个性化推荐摘要"));
        assertTrue(context.promptText().contains("热门景点摘要"));
        assertTrue(context.promptText().contains("最近都在看摘要"));
    }

    private OrderDetailResponse buildOrderDetail() {
        OrderDetailResponse detail = new OrderDetailResponse();
        detail.setOrderNo("T202604121416166706");
        detail.setSpotName("故宫博物院");
        detail.setVisitDate(LocalDate.of(2026, 4, 25));
        detail.setStatusText("已支付");
        detail.setCanPay(false);
        detail.setCanCancel(true);
        return detail;
    }

    private OrderListResponse buildOrderList() {
        OrderListResponse response = new OrderListResponse();
        response.setList(List.of(new OrderListResponse.OrderItem(
                1L,
                "ORD202603020001",
                14L,
                "拙政园",
                null,
                BigDecimal.valueOf(80),
                2,
                BigDecimal.valueOf(160),
                LocalDate.of(2026, 4, 16),
                "paid",
                "已支付",
                null
        )));
        response.setTotal(1L);
        return response;
    }

    private RecommendationResponse buildRecommendations() {
        RecommendationResponse response = new RecommendationResponse();
        response.setNeedPreference(false);
        response.setList(List.of(new RecommendationResponse.SpotItem(
                1L,
                "故宫博物院",
                null,
                BigDecimal.valueOf(60),
                BigDecimal.valueOf(4.7),
                3,
                "历史文化",
                "北京",
                0.91
        )));
        return response;
    }

    private HotSpotResponse buildHotSpots() {
        HotSpotResponse response = new HotSpotResponse();
        response.setList(List.of(new HotSpotResponse.SpotItem(
                5L,
                "西湖",
                null,
                BigDecimal.ZERO,
                BigDecimal.valueOf(4.7),
                9480,
                "自然风光"
        )));
        return response;
    }

    private RecentViewedSpotResponse buildRecentViewed() {
        RecentViewedSpotResponse response = new RecentViewedSpotResponse();
        RecentViewedSpotItem item = new RecentViewedSpotItem();
        item.setName("成都大熊猫繁育研究基地");
        item.setCategoryName("自然风光");
        item.setViewCount(120);
        item.setAvgRating(BigDecimal.valueOf(4.7));
        response.setList(List.of(item));
        return response;
    }
}

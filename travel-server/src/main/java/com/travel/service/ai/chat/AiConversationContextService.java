package com.travel.service.ai.chat;

import com.travel.dto.home.item.RecentViewedSpotItem;
import com.travel.dto.home.response.HotSpotResponse;
import com.travel.dto.home.response.RecentViewedSpotResponse;
import com.travel.dto.order.request.OrderListRequest;
import com.travel.dto.order.response.OrderDetailResponse;
import com.travel.dto.order.response.OrderListResponse;
import com.travel.dto.recommendation.response.RecommendationResponse;
import com.travel.enums.ai.AiScenarioType;
import com.travel.service.OrderService;
import com.travel.service.RecommendationService;
import com.travel.service.ai.intent.AiIntentResult;
import com.travel.service.ai.intent.AiIntentSlots;
import com.travel.service.ai.rule.OrderBusinessRuleProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 对话上下文组装服务。
 *
 * <p>在模型调用前预取少量高价值业务事实，减少模型在已知场景下继续向用户索要系统本可直接查询的信息。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiConversationContextService {

    /**
     * 订单服务。
     */
    private final OrderService orderService;

    /**
     * 推荐服务。
     */
    private final RecommendationService recommendationService;

    /**
     * 订单规则提供器。
     */
    private final OrderBusinessRuleProvider orderBusinessRuleProvider;

    /**
     * 组装当前对话可用的业务上下文。
     *
     * @param userId 当前用户 ID
     * @param scenario 当前场景
     * @param intentResult 意图识别结果
     * @return 上下文快照
     */
    public AiConversationContext assemble(Long userId, AiScenarioType scenario, AiIntentResult intentResult) {
        if (scenario == null) {
            return AiConversationContext.empty();
        }

        Map<String, Object> sections = new LinkedHashMap<>();
        try {
            switch (scenario) {
                case ORDER_ADVISOR -> buildOrderContext(sections, userId, intentResult);
                case TRAVEL_PLANNER, RECOMMENDATION_EXPLAINER -> buildRecommendationContext(sections, userId, scenario);
                default -> {
                    return AiConversationContext.empty();
                }
            }
        } catch (Exception exception) {
            log.warn("AI 对话上下文组装失败：场景={}, 用户ID={}, 原因={}", scenario, userId, exception.getMessage());
            return AiConversationContext.empty();
        }

        if (sections.isEmpty()) {
            return AiConversationContext.empty();
        }
        return new AiConversationContext(sections, renderPromptText(sections));
    }

    /**
     * 组装订单场景上下文。
     *
     * @param sections 上下文分区
     * @param userId 当前用户 ID
     * @param intentResult 意图识别结果
     */
    private void buildOrderContext(Map<String, Object> sections, Long userId, AiIntentResult intentResult) {
        sections.put("订单规则摘要", simplifyOrderRules());
        if (userId == null) {
            return;
        }

        String orderNo = intentResult == null ? "" : intentResult.slotAsString(AiIntentSlots.ORDER_NO);
        if (StringUtils.hasText(orderNo)) {
            OrderDetailResponse detail = orderService.getOrderDetailByOrderNo(userId, orderNo);
            if (detail != null) {
                sections.put("命中订单详情", simplifyOrderDetail(detail));
            }
        }

        OrderListRequest request = new OrderListRequest();
        request.setPage(1);
        request.setPageSize(3);
        OrderListResponse response = orderService.getUserOrders(userId, request);
        if (response != null && response.getList() != null && !response.getList().isEmpty()) {
            sections.put("最近订单摘要", response.getList().stream().limit(3).map(this::simplifyOrderItem).toList());
        }
    }

    /**
     * 组装推荐与规划场景上下文。
     *
     * @param sections 上下文分区
     * @param userId 当前用户 ID
     * @param scenario 当前场景
     */
    private void buildRecommendationContext(Map<String, Object> sections, Long userId, AiScenarioType scenario) {
        if (userId != null) {
            RecommendationResponse recommendations = recommendationService.getRecommendations(userId, 3);
            if (recommendations != null && recommendations.getList() != null && !recommendations.getList().isEmpty()) {
                sections.put("个性化推荐摘要", recommendations.getList().stream().limit(3).map(this::simplifyRecommendationItem).toList());
                if (Boolean.TRUE.equals(recommendations.getNeedPreference())) {
                    sections.put("推荐提示", "当前推荐结果仍可能受偏好信息不足影响。");
                }
            }
        }

        HotSpotResponse hotSpots = recommendationService.getHotSpots(3);
        if (hotSpots != null && hotSpots.getList() != null && !hotSpots.getList().isEmpty()) {
            sections.put("热门景点摘要", hotSpots.getList().stream().limit(3).map(this::simplifyHotSpotItem).toList());
        }

        if (scenario == AiScenarioType.TRAVEL_PLANNER) {
            RecentViewedSpotResponse recentViewed = recommendationService.getRecentViewedSpots(7, 3);
            if (recentViewed != null && recentViewed.getList() != null && !recentViewed.getList().isEmpty()) {
                sections.put("最近都在看摘要", recentViewed.getList().stream().limit(3).map(this::simplifyRecentViewedItem).toList());
            }
        }
    }

    /**
     * 将上下文渲染为模型更容易消费的结构化提示文本。
     *
     * @param sections 上下文分区
     * @return 提示文本
     */
    private String renderPromptText(Map<String, Object> sections) {
        StringBuilder builder = new StringBuilder("\n\n已预取的业务上下文：");
        for (Map.Entry<String, Object> entry : sections.entrySet()) {
            builder.append("\n- ").append(entry.getKey()).append("：").append(entry.getValue());
        }
        builder.append("\n请优先结合这些已知事实回答；如果仍需更多信息，再基于当前结果继续调用工具或做必要澄清。");
        return builder.toString();
    }

    /**
     * 提取订单规则中的高价值摘要，避免把整份规则对象都塞进上下文。
     *
     * @return 精简后的订单规则
     */
    private Map<String, Object> simplifyOrderRules() {
        Map<String, Object> rules = orderBusinessRuleProvider.describeRules();
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("概览", rules.get("generalOverview"));
        summary.put("退款规则", rules.get("refundPolicy"));
        summary.put("声明", rules.get("disclaimer"));
        return summary;
    }

    /**
     * 裁剪订单列表项。
     *
     * @param item 原始订单项
     * @return 精简结果
     */
    private Map<String, Object> simplifyOrderItem(OrderListResponse.OrderItem item) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("orderNo", item.getOrderNo());
        row.put("spotName", item.getSpotName());
        row.put("visitDate", item.getVisitDate());
        row.put("statusText", item.getStatusText());
        return row;
    }

    /**
     * 裁剪订单详情。
     *
     * @param detail 原始订单详情
     * @return 精简结果
     */
    private Map<String, Object> simplifyOrderDetail(OrderDetailResponse detail) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("orderNo", detail.getOrderNo());
        row.put("spotName", detail.getSpotName());
        row.put("visitDate", detail.getVisitDate());
        row.put("statusText", detail.getStatusText());
        row.put("canPay", detail.getCanPay());
        row.put("canCancel", detail.getCanCancel());
        return row;
    }

    /**
     * 裁剪个性化推荐项。
     *
     * @param item 原始推荐项
     * @return 精简结果
     */
    private Map<String, Object> simplifyRecommendationItem(RecommendationResponse.SpotItem item) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("name", item.getName());
        row.put("categoryName", item.getCategoryName());
        row.put("regionName", item.getRegionName());
        row.put("score", item.getScore());
        return row;
    }

    /**
     * 裁剪热门景点项。
     *
     * @param item 原始热门项
     * @return 精简结果
     */
    private Map<String, Object> simplifyHotSpotItem(HotSpotResponse.SpotItem item) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("name", item.getName());
        row.put("categoryName", item.getCategoryName());
        row.put("heatScore", item.getHeatScore());
        row.put("avgRating", item.getAvgRating());
        return row;
    }

    /**
     * 裁剪最近都在看项。
     *
     * @param item 原始浏览项
     * @return 精简结果
     */
    private Map<String, Object> simplifyRecentViewedItem(RecentViewedSpotItem item) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("name", item.getName());
        row.put("categoryName", item.getCategoryName());
        row.put("viewCount", item.getViewCount());
        row.put("avgRating", item.getAvgRating());
        return row;
    }
}

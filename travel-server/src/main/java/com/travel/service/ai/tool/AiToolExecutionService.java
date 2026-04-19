package com.travel.service.ai.tool;

import com.travel.enums.ai.AiScenarioType;
import com.travel.service.ai.intent.AiIntentResult;
import com.travel.service.ai.intent.AiIntentSlots;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * AI 右轨工具预执行服务。
 *
 * <p>该服务根据意图识别结果直接调用一组高价值工具，提前拿到结构化事实，
 * 让最终生成模型优先消费“已知真实数据”，而不是把所有查询都压给最后一跳。</p>
 */
@Service
@RequiredArgsConstructor
public class AiToolExecutionService {

    /**
     * 订单工具。
     */
    private final OrderAiTools orderAiTools;

    /**
     * 景点与攻略工具。
     */
    private final SpotAiTools spotAiTools;

    /**
     * 推荐工具。
     */
    private final RecommendationAiTools recommendationAiTools;

    /**
     * 用户画像工具。
     */
    private final UserProfileAiTools userProfileAiTools;

    /**
     * 运营工具。
     */
    private final OperationAiTools operationAiTools;

    /**
     * 根据场景与意图预执行工具。
     *
     * @param scenario 场景
     * @param intentResult 意图结果
     * @return 结构化工具结果；未命中时返回空结果
     */
    public AiToolExecutionResult execute(AiScenarioType scenario, AiIntentResult intentResult) {
        if (scenario == null || intentResult == null || !intentResult.requiresTool()) {
            return AiToolExecutionResult.empty();
        }
        return switch (scenario) {
            case ORDER_ADVISOR -> executeOrderTools(intentResult);
            case SPOT_QA, GUIDE_QA -> executeSpotTools(intentResult);
            case TRAVEL_PLANNER -> executeTravelPlannerTools(intentResult);
            case RECOMMENDATION_EXPLAINER -> executeRecommendationTools(intentResult);
            case USER_PROFILE_ANALYZER -> AiToolExecutionResult.of("getCurrentUserProfileSummary",
                    userProfileAiTools.getCurrentUserProfileSummary());
            case OPERATION_ANALYZER -> executeOperationTools(intentResult);
            default -> AiToolExecutionResult.empty();
        };
    }

    /**
     * 订单场景工具预执行。
     *
     * @param intentResult 意图结果
     * @return 工具结果
     */
    private AiToolExecutionResult executeOrderTools(AiIntentResult intentResult) {
        String intent = normalizeIntent(intentResult);
        String orderNo = intentResult.slotAsString(AiIntentSlots.ORDER_NO);
        String status = intentResult.slotAsString(AiIntentSlots.STATUS);
        int limit = intentResult.slotAsInt(AiIntentSlots.LIMIT, 5);
        if ("GUIDE_STATUS".equals(intent)) {
            return AiToolExecutionResult.of("getOrderSupportGuide", orderAiTools.getOrderSupportGuide("status"));
        }
        if ("REFUND_ELIGIBILITY".equals(intent)) {
            Map<String, Object> result = new LinkedHashMap<>();
            if (StringUtils.hasText(orderNo)) {
                Map<String, Object> detail = orderAiTools.getOrderDetailByOrderNo(orderNo);
                result.put("orderDetail", detail);
                Long orderId = extractDataLong(detail, "id");
                if (orderId != null) {
                    result.put("afterSalePolicy", orderAiTools.getOrderAfterSalePolicy(orderId));
                }
            } else {
                result.put("supportGuide", orderAiTools.getOrderSupportGuide("refund"));
                result.put("recentOrders", orderAiTools.getMyOrders(status, limit));
            }
            return AiToolExecutionResult.of("refundEligibilityBundle", result);
        }
        if ("DETAIL_BY_ORDER_NO".equals(intent) && StringUtils.hasText(orderNo)) {
            return AiToolExecutionResult.of("getOrderDetailByOrderNo", orderAiTools.getOrderDetailByOrderNo(orderNo));
        }
        if ("LIST_ORDERS".equals(intent)) {
            return AiToolExecutionResult.of("getMyOrders", orderAiTools.getMyOrders(status, limit));
        }
        return AiToolExecutionResult.empty();
    }

    /**
     * 景点与攻略场景工具预执行。
     *
     * @param intentResult 意图结果
     * @return 工具结果
     */
    private AiToolExecutionResult executeSpotTools(AiIntentResult intentResult) {
        String intent = normalizeIntent(intentResult);
        String keyword = firstNonBlank(
                intentResult.slotAsString(AiIntentSlots.KEYWORD),
                intentResult.slotAsString(AiIntentSlots.SPOT_NAME)
        );
        long spotId = intentResult.slotAsLong(AiIntentSlots.SPOT_ID, 0L);
        int limit = intentResult.slotAsInt(AiIntentSlots.LIMIT, 5);
        if ("GUIDE_SEARCH".equals(intent) && StringUtils.hasText(keyword)) {
            return AiToolExecutionResult.of("getGuideSummariesByKeyword",
                    spotAiTools.getGuideSummariesByKeyword(keyword, limit));
        }
        if ("SPOT_FACT".equals(intent)) {
            if (spotId > 0) {
                return AiToolExecutionResult.of("getSpotDetails", spotAiTools.getSpotDetails(spotId));
            }
            if (StringUtils.hasText(keyword)) {
                Map<String, Object> searchResult = spotAiTools.searchSpots(keyword, null, null, 5);
                Long resolvedSpotId = extractFirstListLong(searchResult, "list", "id");
                if (resolvedSpotId != null) {
                    Map<String, Object> result = new LinkedHashMap<>();
                    result.put("searchResult", searchResult);
                    result.put("spotDetail", spotAiTools.getSpotDetails(resolvedSpotId));
                    return AiToolExecutionResult.of("spotFactBundle", result);
                }
                return AiToolExecutionResult.of("searchSpots", searchResult);
            }
        }
        if ("SPOT_SEARCH".equals(intent) && StringUtils.hasText(keyword)) {
            return AiToolExecutionResult.of("searchSpots", spotAiTools.searchSpots(keyword, null, null, limit));
        }
        return AiToolExecutionResult.empty();
    }

    /**
     * 旅行规划场景工具预执行。
     *
     * @param intentResult 意图结果
     * @return 工具结果
     */
    private AiToolExecutionResult executeTravelPlannerTools(AiIntentResult intentResult) {
        Map<String, Object> result = new LinkedHashMap<>();
        int limit = intentResult.slotAsInt(AiIntentSlots.LIMIT, 5);
        result.put("personalizedRecommendations", recommendationAiTools.getPersonalizedRecommendations(limit));
        result.put("hotSpotRecommendations", recommendationAiTools.getHotSpotRecommendations(limit));
        return AiToolExecutionResult.of("travelPlannerBundle", result);
    }

    /**
     * 推荐解释场景工具预执行。
     *
     * @param intentResult 意图结果
     * @return 工具结果
     */
    private AiToolExecutionResult executeRecommendationTools(AiIntentResult intentResult) {
        String intent = normalizeIntent(intentResult);
        long spotId = intentResult.slotAsLong(AiIntentSlots.SPOT_ID, 0L);
        int limit = intentResult.slotAsInt(AiIntentSlots.LIMIT, 5);
        if ("SIMILAR_SPOTS".equals(intent) && spotId > 0) {
            return AiToolExecutionResult.of("getSimilarSpots", recommendationAiTools.getSimilarSpots(spotId, limit));
        }
        if ("SIMILAR_SPOTS".equals(intent)) {
            return AiToolExecutionResult.of("getHotSpotRecommendations",
                    recommendationAiTools.getHotSpotRecommendations(limit));
        }
        return AiToolExecutionResult.of("getPersonalizedRecommendations",
                recommendationAiTools.getPersonalizedRecommendations(limit));
    }

    /**
     * 运营分析场景工具预执行。
     *
     * @param intentResult 意图结果
     * @return 工具结果
     */
    private AiToolExecutionResult executeOperationTools(AiIntentResult intentResult) {
        String intent = normalizeIntent(intentResult);
        if ("HOT_SPOTS".equals(intent)) {
            return AiToolExecutionResult.of("getAdminHotSpots",
                    operationAiTools.getAdminHotSpots(intentResult.slotAsInt(AiIntentSlots.LIMIT, 5)));
        }
        if ("ORDER_TREND".equals(intent)) {
            return AiToolExecutionResult.of("getOrderTrend",
                    operationAiTools.getOrderTrend(intentResult.slotAsInt(AiIntentSlots.DAYS, 7), "range"));
        }
        return AiToolExecutionResult.of("getOperationOverview", operationAiTools.getOperationOverview());
    }

    /**
     * 提取工具结果中的业务数据主键。
     *
     * @param result 工具结果
     * @param key 字段名
     * @return Long 值；不存在时返回 null
     */
    private Long extractDataLong(Map<String, Object> result, String key) {
        if (result == null) {
            return null;
        }
        Object data = result.get("data");
        if (!(data instanceof Map<?, ?> dataMap)) {
            return null;
        }
        Object value = dataMap.get(key);
        if (value instanceof Number number) {
            return number.longValue();
        }
        return null;
    }

    /**
     * 从工具结果列表中提取首个元素的指定 Long 字段。
     *
     * @param result 工具结果
     * @param listKey 列表字段名
     * @param fieldKey 元素字段名
     * @return Long 值；不存在时返回 null
     */
    private Long extractFirstListLong(Map<String, Object> result, String listKey, String fieldKey) {
        if (result == null) {
            return null;
        }
        Object data = result.get("data");
        if (!(data instanceof Map<?, ?> dataMap)) {
            return null;
        }
        Object list = dataMap.get(listKey);
        if (!(list instanceof java.util.List<?> rows) || rows.isEmpty()) {
            return null;
        }
        Object first = rows.get(0);
        if (!(first instanceof Map<?, ?> firstMap)) {
            return null;
        }
        Object value = firstMap.get(fieldKey);
        if (value instanceof Number number) {
            return number.longValue();
        }
        return null;
    }

    /**
     * 归一化意图名，避免空值判断散落。
     *
     * @param intentResult 意图结果
     * @return 归一化意图名
     */
    private String normalizeIntent(AiIntentResult intentResult) {
        return intentResult == null || !StringUtils.hasText(intentResult.intent())
                ? ""
                : intentResult.intent().trim().toUpperCase();
    }

    /**
     * 依次返回第一个非空文本。
     *
     * @param candidates 候选文本
     * @return 非空文本；都为空时返回空字符串
     */
    private String firstNonBlank(String... candidates) {
        if (candidates == null) {
            return "";
        }
        for (String candidate : candidates) {
            if (StringUtils.hasText(candidate)) {
                return candidate.trim();
            }
        }
        return "";
    }

    /**
     * 工具预执行结果。
     */
    public record AiToolExecutionResult(
            boolean executed,
            String toolName,
            Object payload
    ) {

        /**
         * 创建空结果。
         *
         * @return 空结果
         */
        public static AiToolExecutionResult empty() {
            return new AiToolExecutionResult(false, "", null);
        }

        /**
         * 创建成功执行结果。
         *
         * @param toolName 工具名
         * @param payload 结构化结果
         * @return 执行结果
         */
        public static AiToolExecutionResult of(String toolName, Object payload) {
            return new AiToolExecutionResult(true, toolName, payload);
        }
    }
}

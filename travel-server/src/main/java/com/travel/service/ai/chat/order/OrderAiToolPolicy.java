package com.travel.service.ai.chat.order;

import com.travel.service.ai.chat.intent.AiIntentClassificationResult;
import com.travel.service.ai.chat.intent.AiIntentSlots;
import com.travel.service.ai.tool.OrderAiTools;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Map;

/**
 * 订单 AI 工具策略，根据意图包确定性调用订单工具。
 */
@Component
@RequiredArgsConstructor
public class OrderAiToolPolicy {

    private final OrderAiTools orderAiTools;

    /**
     * 执行订单意图对应的工具策略。
     *
     * @param intent 意图类型
     * @param intentResult 全局意图包
     * @return 工具执行结果
     */
    public OrderAiToolResult execute(OrderAiIntent intent, AiIntentClassificationResult intentResult) {
        Map<String, Object> payload = switch (intent) {
            case GUIDE_STATUS -> orderAiTools.getOrderSupportGuide("status");
            case GUIDE_REFUND -> orderAiTools.getOrderSupportGuide("refund");
            case GUIDE_PAGE -> orderAiTools.getOrderSupportGuide("page");
            case LIST_ORDERS -> orderAiTools.getMyOrders(
                    normalizeEmptyToNull(intentResult.slotAsString(AiIntentSlots.STATUS)),
                    intentResult.slotAsInt(AiIntentSlots.LIMIT, 10)
            );
            case REFUND_ELIGIBILITY, DETAIL_BY_ORDER_NO ->
                    orderAiTools.getOrderDetailByOrderNo(intentResult.slotAsString(AiIntentSlots.ORDER_NO));
            default -> Map.of();
        };
        return new OrderAiToolResult(intent, payload);
    }

    private String normalizeEmptyToNull(String value) {
        return StringUtils.hasText(value) ? value : null;
    }
}

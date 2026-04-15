package com.travel.service.ai.chat.order;

import com.travel.service.ai.chat.intent.AiIntentClassificationResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 订单 AI 直答服务，串联订单意图分类、工具策略和回复生成。
 */
@Service
@RequiredArgsConstructor
public class OrderAiDirectResponseService {

    private final OrderAiIntentClassifier orderAiIntentClassifier;
    private final OrderAiToolPolicy orderAiToolPolicy;
    private final OrderAiResponseComposer orderAiResponseComposer;

    /**
     * 尝试生成订单模块确定性回复。
     *
     * @param userMessage 用户问题
     * @return 可直接返回的回复；无法识别订单意图时返回空字符串
     */
    public String tryReply(String userMessage) {
        if (!StringUtils.hasText(userMessage)) {
            return "";
        }
        AiIntentClassificationResult intentResult = orderAiIntentClassifier.classify(userMessage);
        OrderAiIntent orderIntent = parseOrderIntent(intentResult.intent());
        if (orderIntent == OrderAiIntent.NONE) {
            return "";
        }
        OrderAiToolResult toolResult = orderAiToolPolicy.execute(orderIntent, intentResult);
        return orderAiResponseComposer.compose(toolResult);
    }

    private OrderAiIntent parseOrderIntent(String value) {
        if (!StringUtils.hasText(value)) {
            return OrderAiIntent.NONE;
        }
        try {
            return OrderAiIntent.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return OrderAiIntent.NONE;
        }
    }
}

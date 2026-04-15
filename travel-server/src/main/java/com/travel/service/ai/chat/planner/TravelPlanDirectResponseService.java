package com.travel.service.ai.chat.planner;

import com.travel.service.ai.chat.intent.AiIntentClassificationResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 行程规划直答服务，串联意图分类、工具策略和回复生成。
 */
@Service
@RequiredArgsConstructor
public class TravelPlanDirectResponseService {

    private final TravelPlanIntentClassifier travelPlanIntentClassifier;
    private final TravelPlanToolPolicy travelPlanToolPolicy;
    private final TravelPlanResponseComposer travelPlanResponseComposer;

    /**
     * 尝试生成行程规划确定性回复。
     *
     * @param userMessage 用户问题
     * @return 可直接返回的回复；无法识别时返回空字符串
     */
    public String tryReply(String userMessage) {
        if (!StringUtils.hasText(userMessage)) {
            return "";
        }
        AiIntentClassificationResult intentResult = travelPlanIntentClassifier.classify(userMessage);
        TravelPlanIntent intent = parseIntent(intentResult.intent());
        if (intent == TravelPlanIntent.NONE) {
            return "";
        }
        TravelPlanToolResult toolResult = travelPlanToolPolicy.execute(intent, intentResult);
        return travelPlanResponseComposer.compose(toolResult);
    }

    private TravelPlanIntent parseIntent(String value) {
        if (!StringUtils.hasText(value)) {
            return TravelPlanIntent.NONE;
        }
        try {
            return TravelPlanIntent.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return TravelPlanIntent.NONE;
        }
    }
}

package com.travel.service.ai.chat.operation;

import com.travel.service.ai.chat.intent.AiIntentClassificationResult;
import com.travel.service.ai.chat.intent.AiIntentSlots;
import com.travel.service.ai.tool.OperationAiTools;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 运营分析 AI 工具策略。
 */
@Component
@RequiredArgsConstructor
public class OperationAiToolPolicy {

    private final OperationAiTools operationAiTools;

    /**
     * 执行运营分析工具策略。
     *
     * @param intent 意图类型
     * @param intentResult 意图包
     * @return 工具结果
     */
    public OperationAiToolResult execute(OperationAiIntent intent, AiIntentClassificationResult intentResult) {
        int days = intentResult.slotAsInt(AiIntentSlots.DAYS, 7);
        int limit = intentResult.slotAsInt(AiIntentSlots.LIMIT, 5);
        Map<String, Object> payload = switch (intent) {
            case OPERATION_OVERVIEW -> operationAiTools.getOperationOverview();
            case ORDER_TREND -> operationAiTools.getOrderTrend(days, "range");
            case HOT_SPOTS -> operationAiTools.getAdminHotSpots(limit);
            default -> Map.of();
        };
        Map<String, Object> merged = new LinkedHashMap<>(payload);
        merged.put("days", days);
        merged.put("limit", limit);
        return new OperationAiToolResult(intent, merged);
    }
}

package com.travel.service.ai.chat.travel;

import com.travel.service.ai.chat.intent.AiIntentClassificationResult;
import com.travel.service.ai.chat.intent.AiIntentSlots;
import com.travel.service.ai.tool.SpotAiTools;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Map;

/**
 * 旅行内容工具策略，根据意图包确定性调用景点或攻略工具。
 */
@Component
@RequiredArgsConstructor
public class TravelContentToolPolicy {

    private final SpotAiTools spotAiTools;

    /**
     * 执行旅行内容工具策略。
     *
     * @param intent 意图类型
     * @param intentResult 全局意图包
     * @return 工具执行结果
     */
    public TravelContentToolResult execute(TravelContentIntent intent, AiIntentClassificationResult intentResult) {
        String keyword = resolveKeyword(intentResult);
        Integer limit = intentResult.slotAsInt(AiIntentSlots.LIMIT, 5);
        Map<String, Object> payload = switch (intent) {
            case SPOT_SEARCH, SPOT_FACT -> spotAiTools.searchSpots(keyword, null, null, limit);
            case GUIDE_SEARCH -> spotAiTools.getGuideSummariesByKeyword(keyword, limit);
            default -> Map.of();
        };
        return new TravelContentToolResult(intent, payload);
    }

    private String resolveKeyword(AiIntentClassificationResult intentResult) {
        String keyword = intentResult.slotAsString(AiIntentSlots.KEYWORD);
        if (StringUtils.hasText(keyword)) {
            return keyword;
        }
        return intentResult.slotAsString(AiIntentSlots.SPOT_NAME);
    }
}

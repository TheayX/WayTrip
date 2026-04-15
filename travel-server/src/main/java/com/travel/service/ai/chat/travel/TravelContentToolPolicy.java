package com.travel.service.ai.chat.travel;

import com.travel.service.ai.chat.intent.AiIntentClassificationResult;
import com.travel.service.ai.chat.intent.AiIntentSlots;
import com.travel.service.ai.tool.SpotAiTools;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
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
            case SPOT_FACT -> fetchSpotFact(keyword, limit);
            case SPOT_SEARCH -> spotAiTools.searchSpots(keyword, null, null, limit);
            case GUIDE_SEARCH -> spotAiTools.getGuideSummariesByKeyword(keyword, limit);
            default -> Map.of();
        };
        return new TravelContentToolResult(intent, payload);
    }

    private Map<String, Object> fetchSpotFact(String keyword, Integer limit) {
        Map<String, Object> searchPayload = searchWithFallback(keyword, limit);
        Object listValue = searchPayload.get("list");
        if (!(listValue instanceof List<?> list) || list.isEmpty()) {
            return searchPayload;
        }
        Object first = list.get(0);
        if (!(first instanceof Map<?, ?> row) || !(row.get("id") instanceof Number id)) {
            return searchPayload;
        }
        Map<String, Object> payload = new LinkedHashMap<>(searchPayload);
        payload.put("detail", spotAiTools.getSpotDetails(id.longValue()));
        return payload;
    }

    private Map<String, Object> searchWithFallback(String keyword, Integer limit) {
        for (String candidate : TravelContentKeywordNormalizer.buildFallbackKeywords(keyword)) {
            Map<String, Object> payload = spotAiTools.searchSpots(candidate, null, null, limit);
            Object total = payload.get("total");
            if (total instanceof Number number && number.longValue() > 0) {
                return payload;
            }
            Object listValue = payload.get("list");
            if (listValue instanceof List<?> list && !list.isEmpty()) {
                return payload;
            }
        }
        return spotAiTools.searchSpots(TravelContentKeywordNormalizer.normalizeSearchKeyword(keyword), null, null, limit);
    }

    private String resolveKeyword(AiIntentClassificationResult intentResult) {
        String keyword = intentResult.slotAsString(AiIntentSlots.KEYWORD);
        if (StringUtils.hasText(keyword)) {
            return keyword;
        }
        return intentResult.slotAsString(AiIntentSlots.SPOT_NAME);
    }
}

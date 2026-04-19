package com.travel.dto.ai.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI 追问建议项。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiSuggestionItem {

    /**
     * 展示给用户的建议文案。
     */
    private String text;

    /**
     * 点击后优先回传给后端的场景提示。
     */
    private String scenarioHint;

    /**
     * 可选来源页提示，用于辅助后端稳定路由。
     */
    private String sourcePage;
}

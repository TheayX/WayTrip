package com.travel.dto.ai.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI 工具调用摘要。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiToolCallItem {

    private String toolName;
    private String toolCategory;
    private Boolean success;
    private String summary;
}

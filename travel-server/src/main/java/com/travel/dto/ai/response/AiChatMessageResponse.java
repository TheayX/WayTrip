package com.travel.dto.ai.response;

import com.travel.enums.ai.AiScenarioType;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * AI 聊天响应对象。
 */
@Data
public class AiChatMessageResponse {

    private String messageId;
    private String sessionId;
    private AiScenarioType scenario;
    private String reply;
    private List<AiCitationItem> citations = new ArrayList<>();
    private List<AiToolCallItem> toolCalls = new ArrayList<>();
    private List<AiSuggestionItem> suggestions = new ArrayList<>();
    private Boolean feedbackEnabled = Boolean.TRUE;
    private Long createdAt;
}

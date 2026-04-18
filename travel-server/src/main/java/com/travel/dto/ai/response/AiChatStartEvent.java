package com.travel.dto.ai.response;

import com.travel.enums.ai.AiScenarioType;
import lombok.Data;

/**
 * AI 流式回复开始事件。
 */
@Data
public class AiChatStartEvent {

    private String sessionId;
    private String messageId;
    private AiScenarioType scenario;
    private Long createdAt;
}

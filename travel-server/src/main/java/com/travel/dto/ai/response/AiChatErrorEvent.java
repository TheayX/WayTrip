package com.travel.dto.ai.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI 流式回复失败事件。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiChatErrorEvent {

    private String message;
}

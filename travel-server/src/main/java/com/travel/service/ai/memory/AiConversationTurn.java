package com.travel.service.ai.memory;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI 会话单轮消息。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiConversationTurn {

    private String role;
    private String content;
}

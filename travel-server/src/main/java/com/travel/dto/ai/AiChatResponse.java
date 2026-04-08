package com.travel.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * AI 客服对话响应对象。
 */
@Data
@AllArgsConstructor
public class AiChatResponse {

    /**
     * AI 客服回复内容。
     */
    private String reply;
}


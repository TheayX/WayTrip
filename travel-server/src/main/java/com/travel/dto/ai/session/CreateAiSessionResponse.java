package com.travel.dto.ai.session;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * AI 会话创建响应对象。
 */
@Data
@AllArgsConstructor
public class CreateAiSessionResponse {

    private String sessionId;
    private Long createdAt;
}

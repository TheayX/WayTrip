package com.travel.dto.ai.session;

import lombok.Data;

/**
 * AI 会话摘要对象。
 */
@Data
public class AiSessionSummaryResponse {

    private String sessionId;
    private Integer messageCount;
    private Long updatedAt;
    private Boolean exists;
}

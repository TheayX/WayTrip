package com.travel.service.ai;

import com.travel.dto.ai.session.AiSessionSummaryResponse;
import com.travel.dto.ai.session.CreateAiSessionResponse;

/**
 * AI 会话服务接口。
 */
public interface AiSessionService {

    CreateAiSessionResponse createSession();

    AiSessionSummaryResponse getSessionSummary(String sessionId);

    void clearSession(String sessionId);
}

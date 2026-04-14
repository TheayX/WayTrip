package com.travel.service.ai.memory;

import com.travel.dto.ai.session.AiSessionSummaryResponse;
import com.travel.dto.ai.session.CreateAiSessionResponse;
import com.travel.service.ai.AiSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * AI 会话服务实现。
 */
@Service
@RequiredArgsConstructor
public class AiSessionServiceImpl implements AiSessionService {

    private final AiSessionIdService aiSessionIdService;
    private final AiConversationMemoryService aiConversationMemoryService;

    @Override
    public CreateAiSessionResponse createSession() {
        return new CreateAiSessionResponse(aiSessionIdService.createSessionId(), System.currentTimeMillis());
    }

    @Override
    public AiSessionSummaryResponse getSessionSummary(String sessionId) {
        String safeSessionId = aiSessionIdService.normalizeSessionId(sessionId);
        AiSessionSummaryResponse response = new AiSessionSummaryResponse();
        response.setSessionId(safeSessionId);
        response.setExists(aiConversationMemoryService.exists(safeSessionId));
        response.setMessageCount(aiConversationMemoryService.countMessages(safeSessionId));
        response.setUpdatedAt(System.currentTimeMillis());
        return response;
    }

    @Override
    public void clearSession(String sessionId) {
        aiConversationMemoryService.clearSession(aiSessionIdService.normalizeSessionId(sessionId));
    }
}

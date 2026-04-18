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

    /**
     * 会话 ID 生成与校验服务。
     */
    private final AiSessionIdService aiSessionIdService;

    /**
     * 会话记忆读写服务。
     */
    private final AiConversationMemoryService aiConversationMemoryService;

    /**
     * 创建一个新的空白会话，仅生成会话标识和时间戳。
     *
     * @return 会话创建结果
     */
    @Override
    public CreateAiSessionResponse createSession() {
        return new CreateAiSessionResponse(aiSessionIdService.createSessionId(), System.currentTimeMillis());
    }

    /**
     * 返回指定会话当前的存在状态与消息数量摘要。
     *
     * @param sessionId 会话 ID
     * @return 会话摘要
     */
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

    /**
     * 清空指定会话在记忆存储中的内容。
     *
     * @param sessionId 会话 ID
     */
    @Override
    public void clearSession(String sessionId) {
        aiConversationMemoryService.clearSession(aiSessionIdService.normalizeSessionId(sessionId));
    }
}

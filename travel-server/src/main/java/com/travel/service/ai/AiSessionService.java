package com.travel.service.ai;

import com.travel.dto.ai.session.AiSessionSummaryResponse;
import com.travel.dto.ai.session.CreateAiSessionResponse;

/**
 * AI 会话服务接口。
 */
public interface AiSessionService {

    /**
     * 创建新的 AI 会话。
     *
     * @return 会话创建结果
     */
    CreateAiSessionResponse createSession();

    /**
     * 获取会话摘要信息。
     *
     * @param sessionId 会话 ID
     * @return 会话摘要
     */
    AiSessionSummaryResponse getSessionSummary(String sessionId);

    /**
     * 清理指定会话及其上下文。
     *
     * @param sessionId 会话 ID
     */
    void clearSession(String sessionId);
}

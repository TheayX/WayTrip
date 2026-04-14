package com.travel.service.ai.chat;

import com.travel.common.exception.BusinessException;
import com.travel.common.result.ResultCode;
import com.travel.dto.ai.request.AiChatMessageRequest;
import com.travel.dto.ai.response.AiChatMessageResponse;
import com.travel.enums.ai.AiScenarioType;
import com.travel.service.ai.guardrail.AiGuardrailService;
import com.travel.service.ai.memory.AiConversationMemoryService;
import com.travel.service.ai.memory.AiConversationTurn;
import com.travel.service.ai.memory.AiSessionIdService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * AI 对话编排器，集中管理第一阶段聊天主链路。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiConversationOrchestrator {

    private final ChatClient aiChatClient;
    private final AiSessionIdService aiSessionIdService;
    private final AiGuardrailService aiGuardrailService;
    private final AiConversationMemoryService aiConversationMemoryService;
    private final AiScenarioRouter aiScenarioRouter;
    private final AiPromptService aiPromptService;
    private final AiResponseAssembler aiResponseAssembler;

    /**
     * 处理单轮聊天请求。
     *
     * @param request 聊天请求
     * @param userId 当前用户 ID
     * @param clientIp 客户端 IP
     * @return 聊天响应
     */
    public AiChatMessageResponse chat(AiChatMessageRequest request, Long userId, String clientIp) {
        String sessionId = aiSessionIdService.normalizeSessionId(request.getSessionId());
        String userMessage = aiGuardrailService.sanitizeUserMessage(request.getMessage());
        aiGuardrailService.enforceRateLimit(sessionId, clientIp);
        aiGuardrailService.ensureLoginIfNeeded(userMessage, userId);
        aiGuardrailService.ensureNoPromptInjection(userMessage);

        List<AiConversationTurn> history = aiConversationMemoryService.loadHistory(sessionId);
        AiScenarioType scenario = aiScenarioRouter.route(userMessage, request.getScenarioHint(), request.getSourcePage());
        String systemPrompt = aiPromptService.buildSystemPrompt(scenario);
        String userPrompt = aiPromptService.buildUserPrompt(history, userMessage);
        String messageId = aiSessionIdService.createSessionId();

        try {
            String reply = aiChatClient.prompt()
                    .system(systemPrompt)
                    .user(userPrompt)
                    .call()
                    .content();
            if (!StringUtils.hasText(reply)) {
                throw new BusinessException(ResultCode.SYSTEM_ERROR, "AI 返回内容为空");
            }
            aiConversationMemoryService.saveConversation(sessionId, history, userMessage, reply.trim());
            return aiResponseAssembler.assemble(sessionId, messageId, scenario, reply);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("AI 对话执行失败, sessionId={}, scenario={}", sessionId, scenario, e);
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "AI 服务暂时不可用，请稍后重试");
        }
    }
}

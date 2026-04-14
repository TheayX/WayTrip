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
import com.travel.service.ai.rag.AiKnowledgeRetrievalService;
import com.travel.service.ai.rag.AiKnowledgeSnippet;
import com.travel.service.ai.tool.AiToolContextHolder;
import com.travel.service.ai.tool.OrderAiTools;
import com.travel.service.ai.tool.RecommendationAiTools;
import com.travel.service.ai.tool.SpotAiTools;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.retry.NonTransientAiException;
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
    private final AiKnowledgeRetrievalService aiKnowledgeRetrievalService;
    private final AiToolContextHolder aiToolContextHolder;
    private final RecommendationAiTools recommendationAiTools;
    private final OrderAiTools orderAiTools;
    private final SpotAiTools spotAiTools;

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
        List<AiKnowledgeSnippet> knowledgeSnippets = aiKnowledgeRetrievalService.retrieve(scenario, userMessage);
        String systemPrompt = aiPromptService.buildSystemPrompt(scenario);
        String userPrompt = aiPromptService.buildUserPrompt(history, userMessage, knowledgeSnippets);
        String messageId = aiSessionIdService.createSessionId();

        try {
            aiToolContextHolder.setCurrentUserId(userId);
            ChatClient.ChatClientRequestSpec requestSpec = aiChatClient.prompt()
                    .system(systemPrompt)
                    .user(userPrompt);
            if (shouldEnableTools(scenario)) {
                requestSpec = requestSpec.tools(resolveTools(scenario));
            }
            String reply = requestSpec.call().content();
            if (!StringUtils.hasText(reply)) {
                throw new BusinessException(ResultCode.SYSTEM_ERROR, "AI 返回内容为空");
            }
            aiConversationMemoryService.saveConversation(sessionId, history, userMessage, reply.trim());
            return aiResponseAssembler.assemble(
                    sessionId,
                    messageId,
                    scenario,
                    reply,
                    aiToolContextHolder.getToolTraces(),
                    knowledgeSnippets.stream().map(AiKnowledgeSnippet::toCitationItem).toList()
            );
        } catch (BusinessException e) {
            throw e;
        } catch (NonTransientAiException e) {
            log.error("AI 模型调用失败, sessionId={}, scenario={}", sessionId, scenario, e);
            if (e.getMessage() != null && e.getMessage().contains("model") && e.getMessage().contains("not found")) {
                throw new BusinessException(ResultCode.SYSTEM_ERROR, "AI 模型未就绪，请先确认 Ollama 已拉取配置中的模型");
            }
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "AI 服务响应异常，请稍后重试");
        } catch (Exception e) {
            log.error("AI 对话执行失败, sessionId={}, scenario={}", sessionId, scenario, e);
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "AI 服务暂时不可用，请稍后重试");
        } finally {
            aiToolContextHolder.clear();
        }
    }

    private boolean shouldEnableTools(AiScenarioType scenario) {
        return scenario != AiScenarioType.CUSTOMER_SERVICE;
    }

    private Object[] resolveTools(AiScenarioType scenario) {
        return switch (scenario) {
            case ORDER_ADVISOR -> new Object[]{orderAiTools, spotAiTools};
            case RECOMMENDATION_EXPLAINER, TRAVEL_PLANNER -> new Object[]{recommendationAiTools, spotAiTools, orderAiTools};
            case USER_PROFILE_ANALYZER -> new Object[]{recommendationAiTools, orderAiTools};
            case OPERATION_ANALYZER -> new Object[]{recommendationAiTools};
            default -> new Object[0];
        };
    }
}

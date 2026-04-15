package com.travel.service.ai.chat;

import com.travel.common.exception.BusinessException;
import com.travel.common.result.ResultCode;
import com.travel.dto.ai.request.AiChatMessageRequest;
import com.travel.dto.ai.response.AiChatMessageResponse;
import com.travel.enums.ai.AiScenarioType;
import com.travel.service.ai.guardrail.AiGuardrailService;
import com.travel.service.ai.memory.AiSessionIdService;
import com.travel.service.ai.memory.RedisChatMemory;
import com.travel.service.ai.rag.AiKnowledgeRetrievalService;
import com.travel.service.ai.rag.AiKnowledgeSnippet;
import com.travel.service.ai.tool.AiToolContextHolder;
import com.travel.service.ai.tool.OperationAiTools;
import com.travel.service.ai.tool.OrderAiTools;
import com.travel.service.ai.tool.RecommendationAiTools;
import com.travel.service.ai.tool.SpotAiTools;
import com.travel.service.ai.tool.UserProfileAiTools;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.retry.NonTransientAiException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
    private final RedisChatMemory redisChatMemory;
    private final AiScenarioRouter aiScenarioRouter;
    private final AiPromptService aiPromptService;
    private final AiResponseAssembler aiResponseAssembler;
    private final AiKnowledgeRetrievalService aiKnowledgeRetrievalService;
    private final AiToolContextHolder aiToolContextHolder;
    private final RecommendationAiTools recommendationAiTools;
    private final OrderAiTools orderAiTools;
    private final SpotAiTools spotAiTools;
    private final UserProfileAiTools userProfileAiTools;
    private final OperationAiTools operationAiTools;

    /**
     * 处理单轮聊天请求。
     *
     * @param request 聊天请求
     * @param userId 当前用户 ID
     * @param clientIp 客户端 IP
     * @return 聊天响应
     */
    public AiChatMessageResponse chat(AiChatMessageRequest request, Long userId, Long adminId, String clientIp) {
        long startedAt = System.currentTimeMillis();
        String sessionId = aiSessionIdService.normalizeSessionId(request.getSessionId());
        String userMessage = aiGuardrailService.sanitizeUserMessage(request.getMessage());
        aiGuardrailService.enforceRateLimit(sessionId, clientIp);
        aiGuardrailService.ensureLoginIfNeeded(userMessage, userId);
        aiGuardrailService.ensureNoPromptInjection(userMessage);

        AiScenarioType scenario = aiScenarioRouter.route(userMessage, request.getScenarioHint(), request.getSourcePage());
        List<AiKnowledgeSnippet> knowledgeSnippets = aiKnowledgeRetrievalService.retrieve(scenario, userMessage);
        String systemPrompt = aiPromptService.buildSystemPrompt(scenario);
        String messageId = aiSessionIdService.createSessionId();
        log.info(
                "AI 对话开始：会话ID={}, 消息ID={}, 用户ID={}, 场景={}, 来源页面={}, RAG命中数={}, 用户问题预览={}",
                sessionId,
                messageId,
                userId,
                scenario,
                request.getSourcePage(),
                knowledgeSnippets.size(),
                previewText(userMessage, 80)
        );

        try {
            aiToolContextHolder.setCurrentUserId(userId);
            aiToolContextHolder.setCurrentAdminId(adminId);
            
            String userPrompt = aiPromptService.buildUserPrompt(new ArrayList<>(), userMessage, knowledgeSnippets);
            List<Message> history = redisChatMemory.get(sessionId);
            
            ChatClient.ChatClientRequestSpec requestSpec = aiChatClient.prompt()
                    .system(systemPrompt)
                    .messages(history)
                    .user(userPrompt);
            
            if (shouldEnableTools(scenario)) {
                requestSpec = requestSpec.tools(resolveTools(scenario));
            }
            String reply = requestSpec.call().content();
            if (!StringUtils.hasText(reply)) {
                throw new BusinessException(ResultCode.SYSTEM_ERROR, "AI 返回内容为空");
            }
            
            redisChatMemory.add(sessionId, List.of(
                    new UserMessage(userMessage),
                    new AssistantMessage(reply.trim())
            ));
            
            List<String> toolNames = aiToolContextHolder.getToolTraces().stream()
                    .map(item -> item.getToolName() + ":" + item.getSuccess())
                    .collect(Collectors.toList());
            log.info(
                    "AI 对话完成：会话ID={}, 消息ID={}, 场景={}, 是否命中RAG={}, RAG标题={}, 工具调用数量={}, 工具调用详情={}, 回复长度={}, 回复预览={}, 总耗时Ms={}",
                    sessionId,
                    messageId,
                    scenario,
                    !knowledgeSnippets.isEmpty(),
                    knowledgeSnippets.stream().map(AiKnowledgeSnippet::getTitle).toList(),
                    aiToolContextHolder.getToolTraces().size(),
                    toolNames,
                    reply.trim().length(),
                    previewText(reply.trim(), 120),
                    System.currentTimeMillis() - startedAt
            );
            return aiResponseAssembler.assemble(
                    sessionId,
                    messageId,
                    scenario,
                    reply,
                    aiToolContextHolder.getToolTraces(),
                    knowledgeSnippets.stream().map(AiKnowledgeSnippet::toCitationItem).toList()
            );
        } catch (BusinessException e) {
            log.warn(
                    "AI 对话业务拦截：会话ID={}, 消息ID={}, 场景={}, 用户问题预览={}, 总耗时Ms={}, 原因={}",
                    sessionId,
                    messageId,
                    scenario,
                    previewText(userMessage, 80),
                    System.currentTimeMillis() - startedAt,
                    e.getMessage()
            );
            throw e;
        } catch (NonTransientAiException e) {
            log.error(
                    "AI 模型调用失败：会话ID={}, 消息ID={}, 场景={}, 用户问题预览={}, 总耗时Ms={}, 错误={}",
                    sessionId,
                    messageId,
                    scenario,
                    previewText(userMessage, 80),
                    System.currentTimeMillis() - startedAt,
                    e.getMessage(),
                    e
            );
            if (e.getMessage() != null && e.getMessage().contains("model") && e.getMessage().contains("not found")) {
                throw new BusinessException(ResultCode.SYSTEM_ERROR, "AI 模型未就绪，请先确认 Ollama 已拉取配置中的模型");
            }
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "AI 服务响应异常，请稍后重试");
        } catch (Exception e) {
            log.error(
                    "AI 对话执行失败：会话ID={}, 消息ID={}, 场景={}, 用户问题预览={}, 总耗时Ms={}",
                    sessionId,
                    messageId,
                    scenario,
                    previewText(userMessage, 80),
                    System.currentTimeMillis() - startedAt,
                    e
            );
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
            case SPOT_QA, GUIDE_QA -> new Object[]{spotAiTools};
            case RECOMMENDATION_EXPLAINER, TRAVEL_PLANNER -> new Object[]{recommendationAiTools, spotAiTools, orderAiTools};
            case USER_PROFILE_ANALYZER -> new Object[]{userProfileAiTools};
            case OPERATION_ANALYZER -> new Object[]{operationAiTools};
            default -> new Object[]{spotAiTools};
        };
    }

    private String previewText(String text, int maxLength) {
        if (!StringUtils.hasText(text)) {
            return "无";
        }
        String normalized = text.trim().replaceAll("\\s+", " ");
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength) + "...";
    }
}

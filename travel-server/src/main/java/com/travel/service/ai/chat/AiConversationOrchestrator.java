package com.travel.service.ai.chat;

import com.travel.common.exception.BusinessException;
import com.travel.common.result.ResultCode;
import com.travel.dto.ai.request.AiChatMessageRequest;
import com.travel.dto.ai.response.AiChatMessageResponse;
import com.travel.enums.ai.AiScenarioType;
import com.travel.service.ai.chat.order.OrderAiDirectResponseService;
import com.travel.service.ai.chat.planner.TravelPlanDirectResponseService;
import com.travel.service.ai.chat.travel.TravelContentDirectResponseService;
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
    private final AiConversationMemoryService aiConversationMemoryService;
    private final AiScenarioRouter aiScenarioRouter;
    private final OrderAiDirectResponseService orderAiDirectResponseService;
    private final TravelContentDirectResponseService travelContentDirectResponseService;
    private final TravelPlanDirectResponseService travelPlanDirectResponseService;
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
        long startedAt = System.currentTimeMillis();
        String sessionId = aiSessionIdService.normalizeSessionId(request.getSessionId());
        String userMessage = aiGuardrailService.sanitizeUserMessage(request.getMessage());
        aiGuardrailService.enforceRateLimit(sessionId, clientIp);
        aiGuardrailService.ensureLoginIfNeeded(userMessage, userId);
        aiGuardrailService.ensureNoPromptInjection(userMessage);

        List<AiConversationTurn> history = aiConversationMemoryService.loadHistory(sessionId);
        AiScenarioType scenario = aiScenarioRouter.route(userMessage, request.getScenarioHint(), request.getSourcePage());
        List<AiKnowledgeSnippet> knowledgeSnippets = aiKnowledgeRetrievalService.retrieve(scenario, userMessage);
        String systemPrompt = aiPromptService.buildSystemPrompt(scenario);
        String messageId = aiSessionIdService.createSessionId();
        log.info(
                "AI 对话开始：会话ID={}, 消息ID={}, 用户ID={}, 场景={}, 来源页面={}, 历史轮次={}, RAG命中数={}, 用户问题预览={}",
                sessionId,
                messageId,
                userId,
                scenario,
                request.getSourcePage(),
                history.size(),
                knowledgeSnippets.size(),
                previewText(userMessage, 80)
        );

        try {
            aiToolContextHolder.setCurrentUserId(userId);
            AiChatMessageResponse directResponse = tryBuildDirectResponse(
                    sessionId,
                    messageId,
                    scenario,
                    userMessage,
                    knowledgeSnippets
            );
            if (directResponse != null) {
                aiConversationMemoryService.saveConversation(sessionId, history, userMessage, directResponse.getReply());
                log.info(
                        "AI 对话完成：会话ID={}, 消息ID={}, 场景={}, 是否命中RAG={}, RAG标题={}, 工具调用数量={}, 工具调用详情={}, 回复长度={}, 回复预览={}, 总耗时Ms={}, 响应模式=直答",
                        sessionId,
                        messageId,
                        scenario,
                        !knowledgeSnippets.isEmpty(),
                        knowledgeSnippets.stream().map(AiKnowledgeSnippet::getTitle).toList(),
                        aiToolContextHolder.getToolTraces().size(),
                        aiToolContextHolder.getToolTraces().stream().map(item -> item.getToolName() + ":" + item.getSuccess()).toList(),
                        directResponse.getReply().length(),
                        previewText(directResponse.getReply(), 120),
                        System.currentTimeMillis() - startedAt
                    );
                return directResponse;
            }
            String userPrompt = aiPromptService.buildUserPrompt(history, userMessage, knowledgeSnippets);
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
            case USER_PROFILE_ANALYZER -> new Object[]{recommendationAiTools, orderAiTools};
            case OPERATION_ANALYZER -> new Object[]{recommendationAiTools};
            default -> new Object[0];
        };
    }

    /**
     * 对订单模块问题走确定性直答，避免模型绕过工具和业务规则直接编写事实。
     *
     * @param sessionId 会话 ID
     * @param messageId 消息 ID
     * @param scenario 场景类型
     * @param userMessage 用户问题
     * @param knowledgeSnippets RAG 结果
     * @return 可直接返回的响应；若无需直答则返回 null
     */
    private AiChatMessageResponse tryBuildDirectResponse(String sessionId,
                                                         String messageId,
                                                         AiScenarioType scenario,
                                                         String userMessage,
                                                         List<AiKnowledgeSnippet> knowledgeSnippets) {
        if (!StringUtils.hasText(userMessage)) {
            return null;
        }
        String reply = switch (scenario) {
            case ORDER_ADVISOR -> orderAiDirectResponseService.tryReply(userMessage);
            case SPOT_QA, GUIDE_QA -> travelContentDirectResponseService.tryReply(userMessage, scenario);
            case TRAVEL_PLANNER -> travelPlanDirectResponseService.tryReply(userMessage);
            default -> "";
        };
        if (!StringUtils.hasText(reply)) {
            return null;
        }
        return aiResponseAssembler.assemble(
                sessionId,
                messageId,
                scenario,
                reply,
                aiToolContextHolder.getToolTraces(),
                knowledgeSnippets.stream().map(AiKnowledgeSnippet::toCitationItem).toList()
        );
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

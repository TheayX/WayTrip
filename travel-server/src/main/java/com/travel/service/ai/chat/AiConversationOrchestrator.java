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
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * AI 对话编排器，集中管理第一阶段聊天主链路。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiConversationOrchestrator {

    private static final Pattern ORDER_NO_PATTERN = Pattern.compile("\\bT\\d{10,}\\d*\\b", Pattern.CASE_INSENSITIVE);

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
                        "AI 对话完成：会话ID={}, 消息ID={}, 场景={}, 是否命中RAG={}, RAG标题={}, 工具调用数量={}, 工具调用详情={}, 预取事实预览={}, 回复长度={}, 回复预览={}, 总耗时Ms={}, 响应模式=直答",
                        sessionId,
                        messageId,
                        scenario,
                        !knowledgeSnippets.isEmpty(),
                        knowledgeSnippets.stream().map(AiKnowledgeSnippet::getTitle).toList(),
                        aiToolContextHolder.getToolTraces().size(),
                        aiToolContextHolder.getToolTraces().stream().map(item -> item.getToolName() + ":" + item.getSuccess()).toList(),
                        "直答未走预取",
                        directResponse.getReply().length(),
                        previewText(directResponse.getReply(), 120),
                        System.currentTimeMillis() - startedAt
                    );
                return directResponse;
            }
            List<String> prefetchedFacts = prefetchBusinessFacts(scenario, userMessage);
            String userPrompt = aiPromptService.buildUserPrompt(history, userMessage, knowledgeSnippets, prefetchedFacts);
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
                    "AI 对话完成：会话ID={}, 消息ID={}, 场景={}, 是否命中RAG={}, RAG标题={}, 工具调用数量={}, 工具调用详情={}, 预取事实预览={}, 回复长度={}, 回复预览={}, 总耗时Ms={}",
                    sessionId,
                    messageId,
                    scenario,
                    !knowledgeSnippets.isEmpty(),
                    knowledgeSnippets.stream().map(AiKnowledgeSnippet::getTitle).toList(),
                    aiToolContextHolder.getToolTraces().size(),
                    toolNames,
                    previewFacts(prefetchedFacts),
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
            case RECOMMENDATION_EXPLAINER, TRAVEL_PLANNER -> new Object[]{recommendationAiTools, spotAiTools, orderAiTools};
            case USER_PROFILE_ANALYZER -> new Object[]{recommendationAiTools, orderAiTools};
            case OPERATION_ANALYZER -> new Object[]{recommendationAiTools};
            default -> new Object[0];
        };
    }

    private List<String> prefetchBusinessFacts(AiScenarioType scenario, String userMessage) {
        if (scenario != AiScenarioType.ORDER_ADVISOR) {
            return List.of();
        }
        if (!StringUtils.hasText(userMessage)) {
            return List.of();
        }

        String normalized = userMessage.trim();
        if (containsAny(normalized, "订单状态说明", "状态说明")) {
            return List.of(stringifyFact(orderAiTools.getOrderSupportGuide("status")));
        }
        if (containsAny(normalized, "退款流程", "怎么退款", "退款怎么走", "售后流程")) {
            return List.of(stringifyFact(orderAiTools.getOrderSupportGuide("refund")));
        }
        if (containsAny(normalized, "订单页", "看什么", "怎么看订单")) {
            return List.of(stringifyFact(orderAiTools.getOrderSupportGuide("page")));
        }
        String orderNo = extractOrderNo(normalized);
        if (StringUtils.hasText(orderNo)) {
            return List.of(stringifyFact(orderAiTools.getOrderDetailByOrderNo(orderNo)));
        }
        if (containsAny(normalized, "几个订单", "我的订单", "最近订单", "订单列表", "已支付订单", "待支付订单", "已完成订单")) {
            return List.of(stringifyFact(orderAiTools.getMyOrders(resolveOrderStatus(normalized), 5)));
        }
        return List.of();
    }

    /**
     * 对订单通用帮助类问题走确定性直答，避免模型把规则说明误写成当前订单事实。
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
        if (scenario != AiScenarioType.ORDER_ADVISOR || !StringUtils.hasText(userMessage)) {
            return null;
        }
        String guideTopic = resolveOrderGuideTopic(userMessage.trim());
        if (!StringUtils.hasText(guideTopic)) {
            return null;
        }
        Map<String, Object> guide = orderAiTools.getOrderSupportGuide(guideTopic);
        String reply = formatOrderGuideReply(guide);
        return aiResponseAssembler.assemble(
                sessionId,
                messageId,
                scenario,
                reply,
                aiToolContextHolder.getToolTraces(),
                knowledgeSnippets.stream().map(AiKnowledgeSnippet::toCitationItem).toList()
        );
    }

    private String resolveOrderGuideTopic(String userMessage) {
        if (containsAny(userMessage, "订单状态说明", "状态说明")) {
            return "status";
        }
        if (containsAny(userMessage, "退款流程", "怎么退款", "退款怎么走", "售后流程")) {
            return "refund";
        }
        if (containsAny(userMessage, "订单页", "看什么", "怎么看订单")) {
            return "page";
        }
        return null;
    }

    private String formatOrderGuideReply(Map<String, Object> guide) {
        if (guide == null || guide.isEmpty()) {
            return "暂时无法获取订单说明，请稍后重试。";
        }
        String title = Objects.toString(guide.get("title"), "订单说明");
        Object content = guide.get("content");
        if (!(content instanceof List<?> items) || items.isEmpty()) {
            return title;
        }
        String body = items.stream()
                .map(String::valueOf)
                .filter(StringUtils::hasText)
                .collect(Collectors.joining("\n"));
        return title + "：\n" + body;
    }

    private String resolveOrderStatus(String userMessage) {
        if (containsAny(userMessage, "已支付", "待出行")) {
            return "paid";
        }
        if (containsAny(userMessage, "待支付", "未支付")) {
            return "pending";
        }
        if (containsAny(userMessage, "已完成")) {
            return "completed";
        }
        if (containsAny(userMessage, "已取消")) {
            return "cancelled";
        }
        return null;
    }

    private String extractOrderNo(String userMessage) {
        Matcher matcher = ORDER_NO_PATTERN.matcher(userMessage);
        return matcher.find() ? matcher.group() : "";
    }

    private boolean containsAny(String source, String... keywords) {
        for (String keyword : keywords) {
            if (source.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private String stringifyFact(Map<String, Object> fact) {
        return fact == null ? "" : fact.toString();
    }

    private String previewFacts(List<String> facts) {
        if (facts == null || facts.isEmpty()) {
            return "无";
        }
        return facts.stream()
                .map(item -> previewText(item, 120))
                .collect(Collectors.joining(" | "));
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

package com.travel.service.ai.chat;

import com.travel.common.exception.BusinessException;
import com.travel.common.result.ResultCode;
import com.travel.dto.ai.request.AiChatMessageRequest;
import com.travel.dto.ai.response.AiChatMessageResponse;
import com.travel.enums.ai.AiScenarioType;
import com.travel.service.ai.chat.order.OrderAiIntent;
import com.travel.service.ai.chat.order.OrderAiIntentResolver;
import com.travel.service.ai.chat.order.OrderAiIntentResult;
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
    private final OrderAiIntentResolver orderAiIntentResolver;
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
        OrderAiIntentResult intentResult = orderAiIntentResolver.resolve(userMessage);
        if (intentResult.intent() == OrderAiIntent.NONE) {
            return null;
        }
        String reply = buildOrderDirectReply(intentResult);
        return aiResponseAssembler.assemble(
                sessionId,
                messageId,
                scenario,
                reply,
                aiToolContextHolder.getToolTraces(),
                knowledgeSnippets.stream().map(AiKnowledgeSnippet::toCitationItem).toList()
        );
    }

    private String buildOrderDirectReply(OrderAiIntentResult intentResult) {
        return switch (intentResult.intent()) {
            case GUIDE_STATUS -> formatOrderGuideReply(orderAiTools.getOrderSupportGuide("status"));
            case GUIDE_REFUND -> formatOrderGuideReply(orderAiTools.getOrderSupportGuide("refund"));
            case GUIDE_PAGE -> formatOrderGuideReply(orderAiTools.getOrderSupportGuide("page"));
            case LIST_ORDERS -> formatOrderListReply(orderAiTools.getMyOrders(intentResult.status(), intentResult.limit()));
            case DETAIL_BY_ORDER_NO -> formatOrderDetailReply(orderAiTools.getOrderDetailByOrderNo(intentResult.orderNo()));
            default -> "暂时无法确认这个订单问题，请换个问法或稍后重试。";
        };
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

    private String formatOrderListReply(Map<String, Object> orders) {
        long total = parseLong(orders.get("total"));
        Object listValue = orders.get("list");
        if (!(listValue instanceof List<?> list) || list.isEmpty()) {
            return "没有查询到符合条件的订单。你可以到“我的订单”页面确认是否使用了其他账号登录。";
        }
        StringBuilder reply = new StringBuilder("已查询到你的订单，共 ").append(total).append(" 条。");
        reply.append("\n最近订单：");
        for (Object item : list) {
            if (item instanceof Map<?, ?> row) {
                reply.append("\n- ")
                        .append(Objects.toString(row.get("spotName"), "未命名景点"))
                        .append("，状态：")
                        .append(Objects.toString(row.get("statusText"), "未知"))
                        .append("，订单号：")
                        .append(Objects.toString(row.get("orderNo"), "无"))
                        .append("，出行日期：")
                        .append(Objects.toString(row.get("visitDate"), "未设置"))
                        .append("，金额：")
                        .append(Objects.toString(row.get("totalPrice"), "未确认"));
            }
        }
        if (total > list.size()) {
            reply.append("\n当前先展示最近 ").append(list.size()).append(" 条，更多订单请在“我的订单”页面继续查看。");
        }
        return reply.toString();
    }

    private String formatOrderDetailReply(Map<String, Object> detail) {
        if (Boolean.FALSE.equals(detail.get("found"))) {
            return Objects.toString(detail.get("message"), "未找到匹配的订单，请确认订单号是否正确。");
        }
        StringBuilder reply = new StringBuilder("已查询到这笔订单：");
        reply.append("\n- 景点：").append(Objects.toString(detail.get("spotName"), "未命名景点"));
        reply.append("\n- 状态：").append(Objects.toString(detail.get("statusText"), "未知"));
        reply.append("\n- 订单号：").append(Objects.toString(detail.get("orderNo"), "无"));
        reply.append("\n- 出行日期：").append(Objects.toString(detail.get("visitDate"), "未设置"));
        reply.append("\n- 数量：").append(Objects.toString(detail.get("quantity"), "未确认"));
        reply.append("\n- 总金额：").append(Objects.toString(detail.get("totalPrice"), "未确认"));
        if (Boolean.TRUE.equals(detail.get("canPay"))) {
            reply.append("\n下一步：这笔订单当前可以继续支付，具体以订单页按钮为准。");
        } else if (Boolean.TRUE.equals(detail.get("canCancel"))) {
            reply.append("\n下一步：如行程有变，可以到订单页查看取消或售后入口。");
        } else {
            reply.append("\n下一步：建议到订单详情页查看当前可执行操作。");
        }
        return reply.toString();
    }

    private long parseLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(Objects.toString(value, "0"));
        } catch (NumberFormatException e) {
            return 0L;
        }
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

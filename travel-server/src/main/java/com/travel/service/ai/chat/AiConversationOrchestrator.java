package com.travel.service.ai.chat;

import com.travel.common.exception.BusinessException;
import com.travel.common.result.ResultCode;
import com.travel.dto.ai.request.AiChatMessageRequest;
import com.travel.dto.ai.response.AiChatDeltaEvent;
import com.travel.dto.ai.response.AiChatErrorEvent;
import com.travel.dto.ai.response.AiChatMessageResponse;
import com.travel.enums.ai.AiScenarioType;
import com.travel.service.ai.guardrail.AiGuardrailService;
import com.travel.service.ai.memory.AiSessionIdService;
import com.travel.service.ai.memory.RedisChatMemory;
import com.travel.service.ai.rag.AiKnowledgeContextAdvisor;
import com.travel.service.ai.rag.AiKnowledgeRetrievalService;
import com.travel.service.ai.rag.AiKnowledgeSnippet;
import com.travel.service.ai.tool.AiToolContextHolder;
import com.travel.service.ai.tool.AiToolContextHolder.AiToolRequestContext;
import com.travel.service.ai.tool.AiToolRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.retry.NonTransientAiException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * AI 对话编排器，集中管理主聊天链路。
 * <p>
 * 负责串联风控、场景路由、RAG 检索、工具调用与记忆管理，是 Spring AI 能力的核心调度中心。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiConversationOrchestrator {

    /**
     * 统一 AI 聊天客户端。
     */
    private final ChatClient aiChatClient;

    /**
     * Redis 对话记忆实现。
     */
    private final RedisChatMemory redisChatMemory;

    /**
     * 会话 ID 服务。
     */
    private final AiSessionIdService aiSessionIdService;

    /**
     * AI 风控服务。
     */
    private final AiGuardrailService aiGuardrailService;

    /**
     * AI 场景路由器。
     */
    private final AiScenarioRouter aiScenarioRouter;

    /**
     * 系统提示词服务。
     */
    private final AiPromptService aiPromptService;

    /**
     * 响应组装器。
     */
    private final AiResponseAssembler aiResponseAssembler;

    /**
     * RAG 检索服务。
     */
    private final AiKnowledgeRetrievalService aiKnowledgeRetrievalService;

    /**
     * 知识上下文增强 Advisor。
     */
    private final AiKnowledgeContextAdvisor aiKnowledgeContextAdvisor;

    /**
     * 工具上下文持有器。
     */
    private final AiToolContextHolder aiToolContextHolder;

    /**
     * AI 工具注册中心。
     */
    private final AiToolRegistry aiToolRegistry;

    /**
     * AI 流式响应线程池。
     */
    @Qualifier("aiChatStreamExecutor")
    private final TaskExecutor aiChatStreamExecutor;

    /**
     * 处理单轮聊天请求。
     *
     * @param request  聊天请求
     * @param userId   当前用户 ID
     * @param adminId  当前管理员 ID（如有）
     * @param clientIp 客户端 IP
     * @return SSE 发射器
     */
    public SseEmitter chat(AiChatMessageRequest request, Long userId, Long adminId, String clientIp) {
        SseEmitter emitter = new SseEmitter(0L);
        aiChatStreamExecutor.execute(() -> streamChat(emitter, request, userId, adminId, clientIp));
        return emitter;
    }

    /**
     * 在异步线程中执行实际的流式对话逻辑。
     *
     * @param emitter SSE 发射器
     * @param request 聊天请求
     * @param userId 当前用户 ID
     * @param adminId 当前管理员 ID
     * @param clientIp 客户端 IP
     */
    private void streamChat(SseEmitter emitter, AiChatMessageRequest request, Long userId, Long adminId, String clientIp) {
        long startedAt = System.currentTimeMillis();
        // 1. 会话 ID 规范化与输入清洗
        String sessionId = aiSessionIdService.normalizeSessionId(request.getSessionId());
        String userMessage = aiGuardrailService.sanitizeUserMessage(request.getMessage());

        // 2. 风控前置校验：限流、登录态、防注入
        aiGuardrailService.enforceRateLimit(sessionId, clientIp);
        aiGuardrailService.ensureLoginIfNeeded(userMessage, userId);
        aiGuardrailService.ensureNoPromptInjection(userMessage);

        // 3. 场景路由：判断当前是对客服、订单顾问还是推荐解释
        AiScenarioType scenario = aiScenarioRouter.route(userMessage, request.getScenarioHint(), request.getSourcePage());

        // 4. RAG 知识检索：根据场景召回相关文档片段
        List<AiKnowledgeSnippet> knowledgeSnippets = aiKnowledgeRetrievalService.retrieve(scenario, userMessage);

        // 5. 组装系统提示词并生成消息 ID
        String messageId = aiSessionIdService.createSessionId();
        String systemPrompt = aiPromptService.buildSystemPrompt(scenario);
        AiToolRequestContext toolContext = aiToolContextHolder.createContext(userId, adminId);

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
            aiToolContextHolder.withContext(toolContext, () -> {
                sendEvent(emitter, "start", aiResponseAssembler.assembleStartEvent(sessionId, messageId, scenario));

                // 6. 构建 Spring AI 请求链：挂载记忆 Advisor 和 RAG Advisor
                ChatClient.ChatClientRequestSpec requestSpec = aiChatClient.prompt()
                        .advisors(spec -> spec
                                .advisors(
                                        MessageChatMemoryAdvisor.builder(redisChatMemory).conversationId(sessionId).build(),
                                        aiKnowledgeContextAdvisor
                                )
                                .params(Map.of(AiKnowledgeContextAdvisor.CONTEXT_KEY, knowledgeSnippets))
                        )
                        .system(systemPrompt)
                        .user(userMessage);

                // 7. 动态注册工具：不同场景启用不同的业务能力
                Object[] tools = aiToolRegistry.resolveTools(scenario);
                if (tools.length > 0) {
                    requestSpec = requestSpec.tools(tools);
                }

                // 8. 执行模型流式调用并向前端推送增量内容。
                StringBuilder replyBuilder = new StringBuilder();
                AtomicBoolean emittedDelta = new AtomicBoolean(false);
                requestSpec.stream().content().toStream().forEach(delta -> {
                    if (!StringUtils.hasText(delta)) {
                        return;
                    }
                    emittedDelta.set(true);
                    replyBuilder.append(delta);
                    sendEvent(emitter, "delta", new AiChatDeltaEvent(delta));
                });

                if (!emittedDelta.get()) {
                    String fallbackReply = "我暂时无法确认这个问题，请稍后重试。";
                    replyBuilder.append(fallbackReply);
                    sendEvent(emitter, "delta", new AiChatDeltaEvent(fallbackReply));
                }

                String reply = replyBuilder.toString();
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

                AiChatMessageResponse donePayload = aiResponseAssembler.assemble(
                        sessionId,
                        messageId,
                        scenario,
                        reply,
                        aiToolContextHolder.getToolTraces(),
                        knowledgeSnippets.stream().map(AiKnowledgeSnippet::toCitationItem).toList()
                );
                sendEvent(emitter, "done", donePayload);
                emitter.complete();
            });
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
            completeWithError(emitter, e.getMessage());
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
                completeWithError(emitter, "AI 模型未就绪，请先确认 Ollama 已拉取配置中的模型");
                return;
            }
            completeWithError(emitter, "AI 服务响应异常，请稍后重试");
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
            completeWithError(emitter, "AI 服务暂时不可用，请稍后重试");
        }
    }

    /**
     * 向 SSE 连接发送事件；如果发送失败则抛出业务异常统一交给上层处理。
     *
     * @param emitter SSE 发射器
     * @param eventName 事件名
     * @param payload 事件负载
     */
    private void sendEvent(SseEmitter emitter, String eventName, Object payload) {
        try {
            emitter.send(SseEmitter.event().name(eventName).data(payload));
        } catch (Exception exception) {
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "AI 流式响应发送失败");
        }
    }

    /**
     * 向前端发送错误事件并结束当前 SSE 会话。
     *
     * @param emitter SSE 发射器
     * @param message 错误提示
     */
    private void completeWithError(SseEmitter emitter, String message) {
        try {
            emitter.send(SseEmitter.event().name("error").data(new AiChatErrorEvent(message)));
        } catch (Exception ignored) {
            log.warn("AI 错误事件发送失败：{}", message);
        } finally {
            emitter.complete();
        }
    }

    /**
     * 截取文本预览，用于日志打印，避免超长内容刷屏。
     *
     * @param text 原始文本
     * @param maxLength 最大预览长度
     * @return 预览文本
     */
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

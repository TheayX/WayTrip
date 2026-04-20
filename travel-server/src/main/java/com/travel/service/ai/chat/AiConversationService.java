package com.travel.service.ai.chat;

import com.travel.common.exception.BusinessException;
import com.travel.common.result.ResultCode;
import com.travel.config.ai.AiProperties;
import com.travel.dto.ai.request.AiChatMessageRequest;
import com.travel.dto.ai.response.AiChatDeltaEvent;
import com.travel.dto.ai.response.AiChatErrorEvent;
import com.travel.dto.ai.response.AiChatMessageResponse;
import com.travel.enums.ai.AiScenarioType;
import com.travel.service.ai.guardrail.AiGuardrailService;
import com.travel.service.ai.intent.AiIntentResult;
import com.travel.service.ai.intent.AiIntentService;
import com.travel.service.ai.memory.AiSessionIdService;
import com.travel.service.ai.memory.RedisChatMemory;
import com.travel.service.ai.rag.AiKnowledgeContextAdvisor;
import com.travel.service.ai.rag.AiKnowledgeRetrievalService;
import com.travel.service.ai.rag.AiKnowledgeSnippet;
import com.travel.service.ai.tool.AiToolContextHolder;
import com.travel.service.ai.tool.AiToolContextHolder.AiToolRequestContext;
import com.travel.service.ai.tool.AiToolExecutionService;
import com.travel.service.ai.tool.AiToolExecutionService.AiToolExecutionResult;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * AI 对话编排器，集中管理主聊天链路。
 * <p>
 * 负责串联风控、场景路由、RAG 检索、工具调用与记忆管理，是 Spring AI 能力的核心调度中心。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiConversationService {

    /**
     * 统一 AI 聊天客户端。
     */
    private final ChatClient aiChatClient;

    /**
     * AI 模块配置。
     */
    private final AiProperties aiProperties;

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
    private final AiPromptManager aiPromptManager;

    /**
     * 运行时意图识别服务。
     */
    private final AiIntentService aiIntentService;

    /**
     * 响应组装器。
     */
    private final AiResponseAssembler aiResponseAssembler;

    /**
     * 对话上下文融合服务。
     */
    private final AiContextFusionService aiContextFusionService;

    /**
     * 对话业务上下文组装服务。
     */
    private final AiConversationContextService aiConversationContextService;

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
     * AI 工具预执行服务。
     */
    private final AiToolExecutionService aiToolExecutionService;

    /**
     * AI 流式响应线程池。
     */
    @Qualifier("aiChatStreamExecutor")
    private final TaskExecutor aiChatStreamExecutor;

    /**
     * AI 双轨并行执行器。
     */
    @Qualifier("aiChatPipelineExecutor")
    private final TaskExecutor aiChatPipelineExecutor;

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
        AiToolRequestContext toolContext = aiToolContextHolder.createContext(userId, adminId);

        // 4. 双轨并行准备：左轨做 RAG 检索，右轨做意图识别、上下文组装和工具预执行。
        CompletableFuture<List<AiKnowledgeSnippet>> retrievalFuture = buildRetrievalFuture(sessionId, scenario, userMessage);
        CompletableFuture<PreparedConversationData> toolingFuture = buildToolingFuture(sessionId, userId, scenario, userMessage, toolContext);
        CompletableFuture.allOf(retrievalFuture, toolingFuture).join();

        PreparedConversationData preparedData = toolingFuture.join();
        AiIntentResult intentResult = preparedData.intentResult();
        AiConversationContext conversationContext = preparedData.conversationContext();
        AiToolExecutionResult toolExecutionResult = preparedData.toolExecutionResult();
        List<AiKnowledgeSnippet> knowledgeSnippets = retrievalFuture.join();

        // 5. 组装系统提示词并生成消息 ID
        String messageId = aiSessionIdService.createSessionId();
        String systemPrompt = aiContextFusionService.fuse(
                aiPromptManager.buildSystemPrompt(scenario),
                conversationContext,
                intentResult,
                toolExecutionResult
        );

        log.info(
                "AI 对话开始：会话ID={}, 消息ID={}, 用户ID={}, 场景={}, 意图={}, 来源页面={}, 上下文分区数={}, RAG命中数={}, 右轨是否预执行工具={}, 预执行工具名={}, 用户问题预览={}",
                sessionId,
                messageId,
                userId,
                scenario,
                intentResult == null ? "" : intentResult.intent(),
                request.getSourcePage(),
                conversationContext.sections().size(),
                knowledgeSnippets.size(),
                toolExecutionResult.executed(),
                toolExecutionResult.toolName(),
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

                // 7. 动态注册工具：预执行结果作为主事实输入，运行时工具保留给最终模型做补充兜底。
                Object[] tools = aiToolRegistry.resolveTools(scenario);
                if (tools.length > 0) {
                    requestSpec = requestSpec.tools(tools);
                }

                // 8. 执行模型流式调用并向前端推送增量内容。
                StringBuilder replyBuilder = new StringBuilder();
                AtomicBoolean emittedDelta = new AtomicBoolean(false);
                long modelCallStartedAt = System.currentTimeMillis();
                AtomicLong firstDeltaAt = new AtomicLong(0L);
                AtomicLong lastDeltaAt = new AtomicLong(0L);
                log.info(
                        "AI 模型开始生成：会话ID={}, 消息ID={}, 距离请求开始Ms={}, 场景={}",
                        sessionId,
                        messageId,
                        modelCallStartedAt - startedAt,
                        scenario
                );
                requestSpec.stream().content().toStream().forEach(delta -> {
                    if (!StringUtils.hasText(delta)) {
                        return;
                    }
                    long now = System.currentTimeMillis();
                    firstDeltaAt.compareAndSet(0L, now);
                    lastDeltaAt.set(now);
                    // 这里记录模型真实吐出的增量粒度，便于判断当前是否属于“真流式”输出。
                    log.info(
                            "AI 增量分片：会话ID={}, 消息ID={}, 分片长度={}, 分片预览={}",
                            sessionId,
                            messageId,
                            delta.length(),
                            previewText(delta, 40)
                    );
                    emittedDelta.set(true);
                    replyBuilder.append(delta);
                    sendEvent(emitter, "delta", new AiChatDeltaEvent(delta));
                });

                if (!emittedDelta.get()) {
                    String fallbackReply = "我暂时无法确认这个问题，请稍后重试。";
                    replyBuilder.append(fallbackReply);
                    sendEvent(emitter, "delta", new AiChatDeltaEvent(fallbackReply));
                }

                long firstDeltaCostMs = firstDeltaAt.get() == 0L ? -1L : firstDeltaAt.get() - modelCallStartedAt;
                long generationWindowMs = firstDeltaAt.get() == 0L || lastDeltaAt.get() == 0L
                        ? -1L
                        : lastDeltaAt.get() - firstDeltaAt.get();
                log.info(
                        "AI 流式阶段耗时：会话ID={}, 消息ID={}, 模型准备耗时Ms={}, 首字延迟Ms={}, 流式输出窗口Ms={}, 总耗时Ms={}",
                        sessionId,
                        messageId,
                        modelCallStartedAt - startedAt,
                        firstDeltaCostMs,
                        generationWindowMs,
                        System.currentTimeMillis() - startedAt
                );

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
     * 组装右轨预处理结果。
     *
     * @param userId 当前用户 ID
     * @param scenario 当前场景
     * @param userMessage 用户消息
     * @param toolContext 工具上下文
     * @return 预处理结果
     */
    private PreparedConversationData prepareConversationData(Long userId,
                                                             AiScenarioType scenario,
                                                             String userMessage,
                                                             AiToolRequestContext toolContext) {
        AiIntentResult intentResult = aiIntentService.recognize(userMessage, scenario);
        AiConversationContext conversationContext = aiConversationContextService.assemble(userId, scenario, intentResult);
        AiToolExecutionResult toolExecutionResult = aiToolContextHolder.withContext(
                toolContext,
                () -> aiToolExecutionService.execute(scenario, intentResult)
        );
        return new PreparedConversationData(intentResult, conversationContext, toolExecutionResult);
    }

    /**
     * 构建左轨检索任务，并为超时和异常统一降级。
     *
     * @param sessionId 会话 ID
     * @param scenario 当前场景
     * @param userMessage 用户问题
     * @return 检索任务
     */
    private CompletableFuture<List<AiKnowledgeSnippet>> buildRetrievalFuture(String sessionId,
                                                                             AiScenarioType scenario,
                                                                             String userMessage) {
        return CompletableFuture.supplyAsync(
                        () -> aiKnowledgeRetrievalService.retrieve(scenario, userMessage),
                        aiChatPipelineExecutor
                )
                .completeOnTimeout(List.of(), resolveParallelTimeoutMillis(), TimeUnit.MILLISECONDS)
                .exceptionally(exception -> {
                    log.warn("AI 左轨检索失败，已降级为空结果：sessionId={}, scenario={}", sessionId, scenario, exception);
                    return List.of();
                });
    }

    /**
     * 构建右轨工具预处理任务，并为超时和异常统一降级。
     *
     * @param sessionId 会话 ID
     * @param userId 当前用户 ID
     * @param scenario 当前场景
     * @param userMessage 用户问题
     * @param toolContext 工具上下文
     * @return 预处理任务
     */
    private CompletableFuture<PreparedConversationData> buildToolingFuture(String sessionId,
                                                                           Long userId,
                                                                           AiScenarioType scenario,
                                                                           String userMessage,
                                                                           AiToolRequestContext toolContext) {
        return CompletableFuture.supplyAsync(
                        () -> prepareConversationData(userId, scenario, userMessage, toolContext),
                        aiChatPipelineExecutor
                )
                .completeOnTimeout(PreparedConversationData.empty(), resolveParallelTimeoutMillis(), TimeUnit.MILLISECONDS)
                .exceptionally(exception -> {
                    log.warn("AI 右轨工具预执行失败，已降级为仅保留空上下文：sessionId={}, scenario={}", sessionId, scenario, exception);
                    return PreparedConversationData.empty();
                });
    }

    /**
     * 解析双轨并行阶段的统一超时时间。
     *
     * @return 超时毫秒数
     */
    private int resolveParallelTimeoutMillis() {
        Integer value = aiProperties.getChat().getParallelTimeoutMillis();
        return Math.max(500, value == null ? 2500 : value);
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

    /**
     * 右轨预处理结果。
     */
    private record PreparedConversationData(
            AiIntentResult intentResult,
            AiConversationContext conversationContext,
            AiToolExecutionResult toolExecutionResult
    ) {

        /**
         * 创建空结果。
         *
         * @return 空结果
         */
        private static PreparedConversationData empty() {
            return new PreparedConversationData(null, AiConversationContext.empty(), AiToolExecutionResult.empty());
        }
    }
}

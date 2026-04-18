package com.travel.service.ai.chat;

import com.travel.common.exception.BusinessException;
import com.travel.common.result.ResultCode;
import com.travel.dto.ai.request.AiChatMessageRequest;
import com.travel.dto.ai.response.AiChatMessageResponse;
import com.travel.enums.ai.AiScenarioType;
import com.travel.service.ai.guardrail.AiGuardrailService;
import com.travel.service.ai.memory.AiSessionIdService;
import com.travel.service.ai.memory.RedisChatMemory;
import com.travel.service.ai.rag.AiKnowledgeContextAdvisor;
import com.travel.service.ai.rag.AiKnowledgeRetrievalService;
import com.travel.service.ai.rag.AiKnowledgeSnippet;
import com.travel.service.ai.tool.AiToolContextHolder;
import com.travel.service.ai.tool.AiToolRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.retry.NonTransientAiException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
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
     * 处理单轮聊天请求。
     *
     * @param request  聊天请求
     * @param userId   当前用户 ID
     * @param adminId  当前管理员 ID（如有）
     * @param clientIp 客户端 IP
     * @return 聊天响应
     */
    public AiChatMessageResponse chat(AiChatMessageRequest request, Long userId, Long adminId, String clientIp) {
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
            // 6. 设置上下文，供 Tool 内部获取当前操作人信息
            aiToolContextHolder.setCurrentUserId(userId);
            aiToolContextHolder.setCurrentAdminId(adminId);

            // 7. 构建 Spring AI 请求链：挂载记忆 Advisor 和 RAG Advisor
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

            // 8. 动态注册工具：不同场景启用不同的业务能力
            Object[] tools = aiToolRegistry.resolveTools(scenario);
            if (tools.length > 0) {
                requestSpec = requestSpec.tools(tools);
            }

            // 9. 执行模型调用并获取回复
            String reply = requestSpec.call().content();
            if (!StringUtils.hasText(reply)) {
                throw new BusinessException(ResultCode.SYSTEM_ERROR, "AI 返回内容为空");
            }

            // 10. 记录工具调用追踪日志
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

            // 11. 组装最终响应对象（含引用来源与工具摘要）
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
            // 12. 清理 ThreadLocal，防止内存泄漏
            aiToolContextHolder.clear();
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

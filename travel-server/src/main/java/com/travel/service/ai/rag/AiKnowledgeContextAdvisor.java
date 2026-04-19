package com.travel.service.ai.rag;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * AI 知识上下文增强器，在模型调用前将命中的知识片段追加到系统消息，避免污染用户消息历史。
 */
@Component
public class AiKnowledgeContextAdvisor implements CallAdvisor {

    /**
     * 在 Advisor 上下文中传递知识片段的键名。
     */
    public static final String CONTEXT_KEY = "aiKnowledgeSnippets";

    /**
     * 返回 Advisor 名称，便于链路识别和调试。
     *
     * @return Advisor 名称
     */
    @Override
    public String getName() {
        return "aiKnowledgeContextAdvisor";
    }

    /**
     * 将知识增强排在记忆之后执行，保证其基于最终系统消息注入。
     *
     * @return Advisor 顺序
     */
    @Override
    public int getOrder() {
        return DEFAULT_CHAT_MEMORY_PRECEDENCE_ORDER + 50;
    }

    /**
     * 将检索到的知识片段追加到系统消息中，再继续后续模型调用。
     *
     * @param request 原始请求
     * @param chain Advisor 调用链
     * @return 模型响应
     */
    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        Object contextValue = request.context().get(CONTEXT_KEY);
        if (!(contextValue instanceof List<?> rawList) || rawList.isEmpty()) {
            return chain.nextCall(request);
        }

        List<AiKnowledgeSnippet> knowledgeSnippets = rawList.stream()
                .filter(AiKnowledgeSnippet.class::isInstance)
                .map(AiKnowledgeSnippet.class::cast)
                .toList();
        if (knowledgeSnippets.isEmpty()) {
            return chain.nextCall(request);
        }

        Prompt originalPrompt = request.prompt();
        List<Message> messages = new ArrayList<>(originalPrompt.getInstructions());
        int systemMessageIndex = findSystemMessageIndex(messages);
        String knowledgeContext = buildKnowledgeContext(knowledgeSnippets);
        if (!StringUtils.hasText(knowledgeContext)) {
            return chain.nextCall(request);
        }

        if (systemMessageIndex >= 0) {
            SystemMessage systemMessage = (SystemMessage) messages.get(systemMessageIndex);
            messages.set(systemMessageIndex, new SystemMessage(systemMessage.getText() + knowledgeContext));
        } else {
            messages.add(0, new SystemMessage(knowledgeContext.trim()));
        }

        ChatClientRequest enrichedRequest = request.mutate()
                .prompt(new Prompt(messages, originalPrompt.getOptions()))
                .build();
        ChatClientResponse response = chain.nextCall(enrichedRequest);
        return response.mutate().context(CONTEXT_KEY, knowledgeSnippets).build();
    }

    /**
     * 查找提示词中的系统消息位置。
     *
     * @param messages 提示词消息列表
     * @return 系统消息下标；不存在时返回 -1
     */
    private int findSystemMessageIndex(List<Message> messages) {
        for (int index = 0; index < messages.size(); index++) {
            if (messages.get(index) instanceof SystemMessage) {
                return index;
            }
        }
        return -1;
    }

    /**
     * 将知识片段渲染为追加到系统提示词中的引用文本。
     *
     * @param knowledgeSnippets 检索命中的知识片段
     * @return 知识上下文文本
     */
    private String buildKnowledgeContext(List<AiKnowledgeSnippet> knowledgeSnippets) {
        StringBuilder builder = new StringBuilder();
        builder.append("\n\n知识参考（仅在与用户问题相关时使用；优先自然回答，无法确认具体事实时再说明边界）：\n");
        for (AiKnowledgeSnippet snippet : knowledgeSnippets) {
            builder.append("- [")
                    .append(snippet.getTitle())
                    .append("] ")
                    .append(snippet.getSnippet())
                    .append('\n');
        }
        return builder.toString();
    }
}

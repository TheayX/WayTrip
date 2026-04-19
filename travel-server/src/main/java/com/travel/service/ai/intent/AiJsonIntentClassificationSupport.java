package com.travel.service.ai.intent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.util.StringUtils;

/**
 * AI JSON 意图识别辅助类。
 */
public class AiJsonIntentClassificationSupport {

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;

    public AiJsonIntentClassificationSupport(ChatClient chatClient, ObjectMapper objectMapper) {
        this.chatClient = chatClient;
        this.objectMapper = objectMapper;
    }

    /**
     * 调用模型输出 JSON 文本；未配置模型时返回空字符串。
     *
     * @param systemPrompt 系统提示词
     * @param userMessage 用户消息
     * @return 模型原始文本
     */
    public String classify(String systemPrompt, String userMessage) {
        if (chatClient == null) {
            return "";
        }
        return chatClient.prompt()
                .system(systemPrompt)
                .user(userMessage)
                .call()
                .content();
    }

    /**
     * 解析 JSON 文本，兼容 markdown code block。
     *
     * @param content 原始文本
     * @return JSON 节点
     * @throws Exception 解析异常
     */
    public JsonNode parseJson(String content) throws Exception {
        String normalized = unwrapMarkdownJson(content);
        return objectMapper.readTree(normalized);
    }

    /**
     * 剥离 markdown 包裹。
     *
     * @param content 原始内容
     * @return JSON 文本
     */
    public String unwrapMarkdownJson(String content) {
        if (!StringUtils.hasText(content)) {
            return "{}";
        }
        String trimmed = content.trim();
        if (trimmed.startsWith("```")) {
            int firstLineBreak = trimmed.indexOf('\n');
            int lastFence = trimmed.lastIndexOf("```");
            if (firstLineBreak >= 0 && lastFence > firstLineBreak) {
                return trimmed.substring(firstLineBreak + 1, lastFence).trim();
            }
        }
        return trimmed;
    }
}

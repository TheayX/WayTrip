package com.travel.service.ai.chat.intent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.function.Supplier;

/**
 * AI JSON 意图分类公共支撑，统一处理模型调用、JSON 提取、日志和降级。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiJsonIntentClassificationSupport {

    private final ChatClient aiChatClient;
    private final ObjectMapper objectMapper;

    /**
     * 调用模型执行结构化意图分类。
     *
     * @param classifierName 分类器名称
     * @param systemPrompt 分类器系统提示词
     * @param userMessage 用户问题
     * @param fallbackSupplier 降级结果提供器
     * @param parser JSON 解析函数
     * @return 意图分类结果
     * @param <T> 意图结果类型
     */
    public <T> T classify(String classifierName,
                          String systemPrompt,
                          String userMessage,
                          Supplier<T> fallbackSupplier,
                          AiJsonIntentParser<T> parser) {
        T fallback = fallbackSupplier.get();
        if (!StringUtils.hasText(userMessage)) {
            return fallback;
        }
        try {
            String reply = aiChatClient.prompt()
                    .system(systemPrompt)
                    .user("用户问题：" + userMessage.trim())
                    .call()
                    .content();
            T result = parser.parse(parseJsonReply(reply));
            log.info(
                    "{} 意图分类完成：模型原始输出={}, 分类结果={}, 兜底结果={}",
                    classifierName,
                    preview(reply),
                    result,
                    fallback
            );
            return result;
        } catch (Exception e) {
            log.warn("{} 意图分类失败，降级使用兜底结果：用户问题预览={}, 兜底结果={}", classifierName, preview(userMessage), fallback, e);
            return fallback;
        }
    }

    /**
     * 从模型输出中提取并解析 JSON，兼容 Markdown 包裹。
     *
     * @param reply 模型原始输出
     * @return JSON 根节点
     * @throws Exception 解析失败
     */
    public JsonNode parseJsonReply(String reply) throws Exception {
        return objectMapper.readTree(extractJson(reply));
    }

    private String extractJson(String value) {
        if (!StringUtils.hasText(value)) {
            return "{}";
        }
        String trimmed = value.trim();
        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return trimmed.substring(start, end + 1);
        }
        return trimmed;
    }

    private String preview(String value) {
        if (!StringUtils.hasText(value)) {
            return "无";
        }
        String normalized = value.trim().replaceAll("\\s+", " ");
        return normalized.length() <= 120 ? normalized : normalized.substring(0, 120) + "...";
    }
}

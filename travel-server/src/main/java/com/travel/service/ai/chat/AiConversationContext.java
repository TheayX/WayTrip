package com.travel.service.ai.chat;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * AI 对话上下文快照。
 *
 * <p>用于承载模型调用前已经预取到的高价值业务事实，避免上下文工程继续散落在各层实现中。</p>
 */
public record AiConversationContext(
        Map<String, Object> sections,
        String promptText
) {

    /**
     * 创建空上下文。
     *
     * @return 空上下文
     */
    public static AiConversationContext empty() {
        return new AiConversationContext(new LinkedHashMap<>(), "");
    }

    /**
     * 判断当前上下文是否为空。
     *
     * @return 是否为空
     */
    public boolean isEmpty() {
        return promptText == null || promptText.isBlank();
    }
}

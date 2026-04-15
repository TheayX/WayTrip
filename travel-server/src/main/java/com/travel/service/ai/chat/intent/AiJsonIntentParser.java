package com.travel.service.ai.chat.intent;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * AI 结构化意图解析函数，允许各业务模块把 JSON 映射为自己的意图结果。
 *
 * @param <T> 意图结果类型
 */
@FunctionalInterface
public interface AiJsonIntentParser<T> {

    /**
     * 将模型输出的 JSON 节点解析为业务意图结果。
     *
     * @param root JSON 根节点
     * @return 业务意图结果
     * @throws Exception 解析失败
     */
    T parse(JsonNode root) throws Exception;
}

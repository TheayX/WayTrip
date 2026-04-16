package com.travel.service.ai.rule;

import java.util.Map;

/**
 * AI 业务规则提供器，统一向聊天链路暴露结构化规则真相源，避免直接依赖自然语言知识文案。
 */
public interface AiBusinessRuleProvider {

    /**
     * 返回规则域标识。
     *
     * @return 规则域名称
     */
    String getDomain();

    /**
     * 返回当前规则摘要。
     *
     * @return 结构化规则内容
     */
    Map<String, Object> describeRules();
}

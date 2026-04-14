package com.travel.service.ai.rag;

import com.travel.enums.ai.AiKnowledgeDomain;
import com.travel.enums.ai.AiScenarioType;

import java.util.List;

/**
 * AI 知识检索服务接口。
 */
public interface AiKnowledgeRetrievalService {

    /**
     * 按场景和用户问题检索知识片段。
     *
     * @param scenario 场景类型
     * @param userMessage 用户消息
     * @return 命中的知识片段
     */
    List<AiKnowledgeSnippet> retrieve(AiScenarioType scenario, String userMessage);

    /**
     * 根据场景解析默认知识域。
     *
     * @param scenario 场景类型
     * @return 默认知识域
     */
    AiKnowledgeDomain resolveDomain(AiScenarioType scenario);
}

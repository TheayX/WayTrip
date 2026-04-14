package com.travel.service.ai.rag;

import com.travel.dto.ai.knowledge.ManualAiKnowledgeUpsertRequest;

/**
 * AI 知识导入服务接口。
 */
public interface AiKnowledgeIngestionService {

    /**
     * 手工导入知识文档并重建分片。
     *
     * @param request 导入请求
     * @return 文档 ID
     */
    Long upsertManualDocument(ManualAiKnowledgeUpsertRequest request);

    /**
     * 基于现有文档重新构建分片。
     *
     * @param documentId 文档 ID
     */
    void rebuildChunks(Long documentId);
}

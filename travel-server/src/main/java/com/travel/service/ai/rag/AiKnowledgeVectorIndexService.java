package com.travel.service.ai.rag;

import com.travel.entity.AiKnowledgeChunk;
import com.travel.entity.AiKnowledgeDocument;

import java.util.List;

/**
 * AI 知识向量索引服务。
 */
public interface AiKnowledgeVectorIndexService {

    /**
     * 重建指定文档的向量索引。
     *
     * @param document 文档实体
     * @param chunks 文档分片
     */
    void rebuildDocumentIndex(AiKnowledgeDocument document, List<AiKnowledgeChunk> chunks);

    /**
     * 清空当前 AI 知识向量数据。
     *
     * @return 清理掉的向量数量
     */
    int clearAllVectorData();

    /**
     * 按当前 embedding 模型重新创建向量索引结构。
     */
    void recreateIndexSchema();
}

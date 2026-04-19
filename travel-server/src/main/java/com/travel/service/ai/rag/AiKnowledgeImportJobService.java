package com.travel.service.ai.rag;

/**
 * AI 知识导入任务服务。
 */
public interface AiKnowledgeImportJobService {

    /**
     * 将指定文档的分片与向量构建任务加入后台执行队列。
     *
     * @param documentId 文档 ID
     */
    void enqueueDocumentRebuild(Long documentId);
}

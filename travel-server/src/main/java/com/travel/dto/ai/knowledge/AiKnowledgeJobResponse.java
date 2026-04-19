package com.travel.dto.ai.knowledge;

import lombok.Data;

/**
 * AI 知识任务响应。
 */
@Data
public class AiKnowledgeJobResponse {

    private Long documentId;
    private String indexStatus;
    private Integer clearedVectorCount;
    private Integer queuedDocumentCount;
    private Integer queuedChunkCount;
    private String message;
}

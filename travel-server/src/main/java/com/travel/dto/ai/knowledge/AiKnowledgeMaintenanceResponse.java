package com.travel.dto.ai.knowledge;

import lombok.Data;

/**
 * AI 知识向量维护结果。
 */
@Data
public class AiKnowledgeMaintenanceResponse {

    private Integer clearedVectorCount;
    private Integer rebuiltDocumentCount;
    private Integer rebuiltChunkCount;
    private String message;
}

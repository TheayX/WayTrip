package com.travel.enums.ai;

/**
 * AI 知识分片向量状态。
 */
public enum AiKnowledgeEmbeddingStatus {

    PENDING(0),
    SUCCESS(1),
    FAILED(2);

    private final int code;

    AiKnowledgeEmbeddingStatus(int code) {
        this.code = code;
    }

    public int code() {
        return code;
    }
}

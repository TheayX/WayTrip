package com.travel.service.ai.rag;

import com.travel.enums.ai.AiKnowledgeEmbeddingStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * AI 知识分片向量状态测试。
 */
class AiKnowledgeEmbeddingStatusTest {

    @Test
    void shouldExposeStableCodes() {
        assertEquals(0, AiKnowledgeEmbeddingStatus.PENDING.code());
        assertEquals(1, AiKnowledgeEmbeddingStatus.SUCCESS.code());
        assertEquals(2, AiKnowledgeEmbeddingStatus.FAILED.code());
    }
}

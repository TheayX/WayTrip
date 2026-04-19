package com.travel.dto.ai.knowledge;

import com.travel.service.ai.rag.AiKnowledgeSnippet;
import lombok.Data;

import java.util.List;

/**
 * AI 知识文档详情响应。
 */
@Data
public class AiKnowledgeDocumentDetailResponse {

    private Long id;
    private String title;
    private String knowledgeDomain;
    private String sourceType;
    private String sourceRef;
    private String content;
    private String tags;
    private Integer version;
    private Integer isEnabled;
    private Integer chunkCount;
    private String indexStatus;
    private Integer pendingChunkCount;
    private Integer failedChunkCount;
    private List<AiKnowledgeSnippet> chunks;
    private String updatedAt;
}

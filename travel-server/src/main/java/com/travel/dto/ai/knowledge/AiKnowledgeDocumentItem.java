package com.travel.dto.ai.knowledge;

import lombok.Data;

/**
 * AI 知识文档摘要项。
 */
@Data
public class AiKnowledgeDocumentItem {

    private Long id;
    private String title;
    private String knowledgeDomain;
    private String sourceType;
    private String sourceRef;
    private Integer version;
    private Integer isEnabled;
    private Integer chunkCount;
    private String updatedAt;
}

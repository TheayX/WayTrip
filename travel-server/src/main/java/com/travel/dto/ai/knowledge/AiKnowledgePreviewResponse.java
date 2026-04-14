package com.travel.dto.ai.knowledge;

import com.travel.service.ai.rag.AiKnowledgeSnippet;
import lombok.Data;

import java.util.List;

/**
 * AI 知识检索预览响应对象。
 */
@Data
public class AiKnowledgePreviewResponse {

    private String query;
    private String scenario;
    private String domain;
    private Integer hitCount;
    private List<AiKnowledgeSnippet> hits;
}

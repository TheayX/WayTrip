package com.travel.service.ai.rag;

import com.travel.dto.ai.response.AiCitationItem;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI 检索命中的知识片段。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiKnowledgeSnippet {

    private Long documentId;
    private Long chunkId;
    private String title;
    private String sourceType;
    private String sourceRef;
    private String snippet;
    private String knowledgeDomain;

    /**
     * 转换为响应引用项。
     *
     * @return 引用项
     */
    public AiCitationItem toCitationItem() {
        return new AiCitationItem(title, sourceType, sourceRef, snippet);
    }
}

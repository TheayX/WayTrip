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

    /**
     * 来源文档 ID。
     */
    private Long documentId;

    /**
     * 命中的知识分片 ID。
     */
    private Long chunkId;

    /**
     * 文档标题。
     */
    private String title;

    /**
     * 来源类型，例如 manual。
     */
    private String sourceType;

    /**
     * 来源引用标识，例如外部链接或来源说明。
     */
    private String sourceRef;

    /**
     * 命中的片段正文。
     */
    private String snippet;

    /**
     * 归属知识域。
     */
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

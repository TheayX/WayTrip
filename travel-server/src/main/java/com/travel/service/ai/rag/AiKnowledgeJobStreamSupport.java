package com.travel.service.ai.rag;

/**
 * AI 知识任务 Stream 常量。
 */
public final class AiKnowledgeJobStreamSupport {

    public static final String STREAM_KEY = "waytrip:ai:knowledge:jobs";
    public static final String GROUP = "ai-knowledge-group";
    public static final String CONSUMER = "ai-knowledge-consumer";
    public static final String FIELD_DOCUMENT_ID = "documentId";
    public static final String FIELD_TYPE = "type";
    public static final String TYPE_REBUILD_DOCUMENT = "REBUILD_DOCUMENT";

    private AiKnowledgeJobStreamSupport() {
    }
}

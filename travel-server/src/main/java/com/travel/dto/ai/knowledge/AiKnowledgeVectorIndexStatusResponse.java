package com.travel.dto.ai.knowledge;

import lombok.Data;

/**
 * AI 向量索引运行状态。
 */
@Data
public class AiKnowledgeVectorIndexStatusResponse {

    private Boolean ragEnabled;
    private String chatProvider;
    private String embeddingProvider;
    private Boolean mixedProviderMode;
    private String chatModel;
    private String embeddingModel;
    private String redisHost;
    private Integer redisPort;
    private String indexName;
    private String prefix;
    private Integer modelDimension;
    private Integer indexDimension;
    private Boolean indexExists;
    private Boolean dimensionMatched;
    private Boolean retrievalReady;
    private Boolean needsRebuild;
    private String warningMessage;
    private Integer documentCount;
    private Integer enabledDocumentCount;
    private Integer totalChunkCount;
    private Integer pendingChunkCount;
    private Integer completedChunkCount;
    private Integer failedChunkCount;
}

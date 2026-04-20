package com.travel.config.ai;

import lombok.Getter;

/**
 * AI 向量索引健康检查结果。
 */
@Getter
public class AiVectorIndexHealth {

    /**
     * 是否启用了 RAG。
     */
    private final boolean ragEnabled;

    /**
     * 当前索引是否存在。
     */
    private final boolean indexExists;

    /**
     * 当前 embedding 模型维度。
     */
    private final Integer modelDimension;

    /**
     * 当前 Redis 索引维度。
     */
    private final Integer indexDimension;

    /**
     * 索引维度是否与模型一致。
     */
    private final boolean dimensionMatched;

    /**
     * 当前是否允许进入向量检索链路。
     */
    private final boolean retrievalReady;

    /**
     * 当前是否需要执行索引重建。
     */
    private final boolean needsRebuild;

    /**
     * 面向管理端和日志输出的状态说明。
     */
    private final String warningMessage;

    public AiVectorIndexHealth(
            boolean ragEnabled,
            boolean indexExists,
            Integer modelDimension,
            Integer indexDimension,
            boolean dimensionMatched,
            boolean retrievalReady,
            boolean needsRebuild,
            String warningMessage) {
        this.ragEnabled = ragEnabled;
        this.indexExists = indexExists;
        this.modelDimension = modelDimension;
        this.indexDimension = indexDimension;
        this.dimensionMatched = dimensionMatched;
        this.retrievalReady = retrievalReady;
        this.needsRebuild = needsRebuild;
        this.warningMessage = warningMessage;
    }
}

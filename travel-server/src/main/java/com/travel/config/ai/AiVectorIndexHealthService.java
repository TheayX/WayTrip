package com.travel.config.ai;

import com.travel.service.ai.rag.AiVectorIndexInfoSupport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Component;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.exceptions.JedisDataException;

/**
 * AI 向量索引健康检查服务。
 * <p>
 * 统一收口“模型维度、索引维度、索引是否存在、当前能否检索”这类判断，
 * 避免启动校验、管理端状态页和在线检索各自维护一套分叉逻辑。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiVectorIndexHealthService {

    /**
     * AI 模块配置。
     */
    private final AiProperties aiProperties;

    /**
     * 当前 embedding 模型。
     */
    private final EmbeddingModel embeddingModel;

    /**
     * AI 向量 Redis 连接。
     */
    private final JedisPooled aiVectorJedisPooled;

    /**
     * 向量索引信息解析工具。
     */
    private final AiVectorIndexInfoSupport aiVectorIndexInfoSupport;

    /**
     * 检查当前向量索引是否处于可检索状态。
     *
     * @return 当前健康检查结果
     */
    public AiVectorIndexHealth inspect() {
        if (!Boolean.TRUE.equals(aiProperties.getRag().getEnabled())) {
            return new AiVectorIndexHealth(false, false, null, null, false, false, false, "");
        }

        String indexName = aiProperties.getVector().getRedis().getIndexName();
        Integer modelDimension = resolveModelDimension();
        Integer indexDimension = resolveIndexDimension(indexName);
        boolean indexExists = indexDimension != null && indexDimension > 0;
        boolean dimensionMatched = modelDimension != null && indexDimension != null
                && modelDimension.intValue() == indexDimension.intValue();

        if (modelDimension == null) {
            return new AiVectorIndexHealth(
                    true,
                    indexExists,
                    null,
                    indexDimension,
                    false,
                    false,
                    false,
                    "当前无法获取 embedding 模型维度，已暂时跳过向量检索，请先确认本地向量模型服务可用。"
            );
        }

        if (!indexExists) {
            return new AiVectorIndexHealth(
                    true,
                    false,
                    modelDimension,
                    null,
                    false,
                    false,
                    true,
                    String.format("当前未检测到 Redis 向量索引 %s，需要先执行一次“清空后重建 AI 知识向量”。", indexName)
            );
        }

        if (!dimensionMatched) {
            return new AiVectorIndexHealth(
                    true,
                    true,
                    modelDimension,
                    indexDimension,
                    false,
                    false,
                    true,
                    String.format(
                            "当前 embedding 模型维度为 %d，但 Redis 向量索引 %s 的维度为 %d。请执行“清空后重建 AI 知识向量”。",
                            modelDimension,
                            indexName,
                            indexDimension
                    )
            );
        }

        return new AiVectorIndexHealth(true, true, modelDimension, indexDimension, true, true, false, "");
    }

    /**
     * 读取当前 embedding 模型维度。
     *
     * @return 模型维度；读取失败时返回 {@code null}
     */
    private Integer resolveModelDimension() {
        try {
            return embeddingModel.dimensions();
        } catch (Exception exception) {
            log.warn("获取当前 embedding 模型维度失败", exception);
            return null;
        }
    }

    /**
     * 读取 Redis 向量索引维度。
     *
     * @param indexName 索引名称
     * @return 索引维度；索引不存在或读取失败时返回 {@code null}
     */
    private Integer resolveIndexDimension(String indexName) {
        try {
            return aiVectorIndexInfoSupport.extractDimension(aiVectorJedisPooled.ftInfo(indexName));
        } catch (JedisDataException exception) {
            if (exception.getMessage() != null && exception.getMessage().contains("Unknown Index name")) {
                return null;
            }
            log.warn("读取 Redis 向量索引维度失败:indexName={}", indexName, exception);
            return null;
        } catch (Exception exception) {
            log.warn("读取 Redis 向量索引维度失败:indexName={}", indexName, exception);
            return null;
        }
    }
}

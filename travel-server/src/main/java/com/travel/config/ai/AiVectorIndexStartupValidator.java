package com.travel.config.ai;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.travel.entity.AiKnowledgeChunk;
import com.travel.mapper.AiKnowledgeChunkMapper;
import com.travel.service.ai.rag.AiVectorIndexInfoSupport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.exceptions.JedisDataException;

import java.util.Map;

/**
 * 启动时校验当前 embedding 模型维度与 Redis 向量索引是否一致，避免请求阶段才暴露维度错误。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiVectorIndexStartupValidator implements ApplicationRunner {

    /**
     * AI 模块配置，用于读取 RAG 开关和索引名称。
     */
    private final AiProperties aiProperties;

    /**
     * 当前生效的 embedding 模型，用于获取向量维度。
     */
    private final EmbeddingModel embeddingModel;

    /**
     * AI 向量 Redis 连接，用于读取 FT.INFO 索引信息。
     */
    private final JedisPooled aiVectorJedisPooled;

    /**
     * 知识分片持久层，用于统计已向量化数据量并输出错误上下文。
     */
    private final AiKnowledgeChunkMapper aiKnowledgeChunkMapper;

    /**
     * 向量索引信息解析工具。
     */
    private final AiVectorIndexInfoSupport aiVectorIndexInfoSupport;

    /**
     * 在应用启动阶段校验 embedding 模型维度与 Redis 向量索引维度是否一致。
     * <p>
     * 这样可以把“模型切换后未重建索引”这类问题提前暴露在启动期，
     * 避免首个线上请求才触发维度不匹配异常。
     *
     * @param args 启动参数
     */
    @Override
    public void run(ApplicationArguments args) {
        if (!Boolean.TRUE.equals(aiProperties.getRag().getEnabled())) {
            return;
        }

        String indexName = aiProperties.getVector().getRedis().getIndexName();
        Integer actualDimension = resolveIndexDimension(indexName);
        if (actualDimension == null) {
            return;
        }

        int expectedDimension;
        try {
            expectedDimension = embeddingModel.dimensions();
        } catch (Exception exception) {
            throw new IllegalStateException("AI 启动校验失败：无法获取当前 embedding 模型维度，请确认向量模型服务可用后再启动。", exception);
        }

        if (actualDimension == expectedDimension) {
            log.info("AI 向量索引启动校验通过：indexName={}, expectedDimension={}, actualDimension={}", indexName, expectedDimension, actualDimension);
            return;
        }

        int completedChunkCount = countCompletedChunks();
        String message = String.format(
                "AI 启动校验失败：当前 embedding 模型维度为 %d，但 Redis 向量索引 %s 的维度为 %d。"
                        + "这通常意味着你切换了 embedding 模型后还没有重建知识库向量。"
                        + "请执行“清空后重建 AI 知识向量”接口，再重新启动服务。已完成向量化的分片数量=%d。",
                expectedDimension,
                indexName,
                actualDimension,
                completedChunkCount
        );
        throw new IllegalStateException(message);
    }

    /**
     * 从 RedisSearch 索引信息中解析向量维度。
     *
     * @param indexName RedisSearch 索引名称
     * @return 索引维度；索引不存在时返回 {@code null}
     */
    private Integer resolveIndexDimension(String indexName) {
        Map<String, Object> indexInfo;
        try {
            indexInfo = aiVectorJedisPooled.ftInfo(indexName);
        } catch (JedisDataException exception) {
            if (exception.getMessage() != null && exception.getMessage().contains("Unknown Index name")) {
                log.info("AI 向量索引启动校验跳过：indexName={}, reason=index_not_found", indexName);
                return null;
            }
            throw new IllegalStateException("AI 启动校验失败：读取 Redis 向量索引信息异常。", exception);
        } catch (Exception exception) {
            throw new IllegalStateException("AI 启动校验失败：无法连接 AI 向量 Redis 或读取索引信息。", exception);
        }

        Integer dimension = aiVectorIndexInfoSupport.extractDimension(indexInfo);
        if (dimension == null || dimension <= 0) {
            throw new IllegalStateException("AI 启动校验失败：无法从 Redis 向量索引信息中解析维度，请检查索引结构是否正常。");
        }
        return dimension;
    }

    /**
     * 统计已经完成向量化的知识分片数量，便于在启动失败时评估重建影响范围。
     *
     * @return 已完成向量化的分片数
     */
    private int countCompletedChunks() {
        Long count = aiKnowledgeChunkMapper.selectCount(
                new LambdaQueryWrapper<AiKnowledgeChunk>()
                        .eq(AiKnowledgeChunk::getEmbeddingStatus, 1)
        );
        return count == null ? 0 : count.intValue();
    }
}

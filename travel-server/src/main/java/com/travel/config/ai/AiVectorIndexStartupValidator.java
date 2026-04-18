package com.travel.config.ai;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.travel.entity.AiKnowledgeChunk;
import com.travel.mapper.AiKnowledgeChunkMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.exceptions.JedisDataException;

import java.util.List;
import java.util.Map;

/**
 * 启动时校验当前 embedding 模型维度与 Redis 向量索引是否一致，避免请求阶段才暴露维度错误。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiVectorIndexStartupValidator implements ApplicationRunner {

    private final AiProperties aiProperties;
    private final EmbeddingModel embeddingModel;
    private final JedisPooled aiVectorJedisPooled;
    private final AiKnowledgeChunkMapper aiKnowledgeChunkMapper;

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

        Integer dimension = extractDimension(indexInfo);
        if (dimension == null || dimension <= 0) {
            throw new IllegalStateException("AI 启动校验失败：无法从 Redis 向量索引信息中解析维度，请检查索引结构是否正常。");
        }
        return dimension;
    }

    /**
     * Redis FT.INFO 返回结构在不同客户端版本下可能包含 Map 或 List，这里统一做递归解析。
     */
    private Integer extractDimension(Object source) {
        if (source instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if ("DIM".equalsIgnoreCase(String.valueOf(entry.getKey()))) {
                    Integer dimension = toInteger(entry.getValue());
                    if (dimension != null) {
                        return dimension;
                    }
                }
                Integer nested = extractDimension(entry.getValue());
                if (nested != null) {
                    return nested;
                }
            }
            return null;
        }

        if (source instanceof List<?> list) {
            for (int i = 0; i < list.size(); i++) {
                Object current = list.get(i);
                if ("DIM".equalsIgnoreCase(String.valueOf(current)) && i + 1 < list.size()) {
                    Integer dimension = toInteger(list.get(i + 1));
                    if (dimension != null) {
                        return dimension;
                    }
                }
                Integer nested = extractDimension(current);
                if (nested != null) {
                    return nested;
                }
            }
        }
        return null;
    }

    private Integer toInteger(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String stringValue) {
            try {
                return Integer.parseInt(stringValue.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private int countCompletedChunks() {
        Long count = aiKnowledgeChunkMapper.selectCount(
                new LambdaQueryWrapper<AiKnowledgeChunk>()
                        .eq(AiKnowledgeChunk::getEmbeddingStatus, 1)
        );
        return count == null ? 0 : count.intValue();
    }
}

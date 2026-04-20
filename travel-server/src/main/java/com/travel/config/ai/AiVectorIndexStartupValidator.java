package com.travel.config.ai;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.travel.entity.AiKnowledgeChunk;
import com.travel.mapper.AiKnowledgeChunkMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 启动时检查向量索引状态，并在异常时降级为告警日志。
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
     * 向量索引健康检查服务。
     */
    private final AiVectorIndexHealthService aiVectorIndexHealthService;

    /**
     * 知识分片持久层，用于输出当前已向量化数据量。
     */
    private final AiKnowledgeChunkMapper aiKnowledgeChunkMapper;

    /**
     * 在应用启动阶段检查向量索引状态。
     * <p>
     * 这里不再因为索引维度不匹配而直接中断启动，而是改为输出告警并让管理端继续承接修复。
     *
     * @param args 启动参数
     */
    @Override
    public void run(ApplicationArguments args) {
        if (!Boolean.TRUE.equals(aiProperties.getRag().getEnabled())) {
            return;
        }

        AiVectorIndexHealth health = aiVectorIndexHealthService.inspect();
        if (health.isRetrievalReady()) {
            log.info(
                    "AI 向量索引启动检查通过：indexName={}, expectedDimension={}, actualDimension={}",
                    aiProperties.getVector().getRedis().getIndexName(),
                    health.getModelDimension(),
                    health.getIndexDimension()
            );
            return;
        }

        if (health.getWarningMessage() == null || health.getWarningMessage().isBlank()) {
            return;
        }
        log.warn(
                "AI 向量索引启动检查告警：warning={}, completedChunkCount={}",
                health.getWarningMessage(),
                countCompletedChunks()
        );
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

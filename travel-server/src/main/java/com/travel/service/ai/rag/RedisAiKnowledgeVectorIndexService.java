package com.travel.service.ai.rag;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.travel.entity.AiKnowledgeChunk;
import com.travel.entity.AiKnowledgeDocument;
import com.travel.mapper.AiKnowledgeChunkMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 基于 Redis Vector Store 的 AI 知识向量索引服务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisAiKnowledgeVectorIndexService implements AiKnowledgeVectorIndexService {

    /**
     * 向量文档 ID 前缀。
     */
    private static final String VECTOR_ID_PREFIX = "ai_chunk_";

    /**
     * 向量存储入口。
     */
    private final VectorStore vectorStore;

    /**
     * 知识分片持久层。
     */
    private final AiKnowledgeChunkMapper aiKnowledgeChunkMapper;

    /**
     * 重建指定文档的全部向量索引，并同步更新分片状态。
     *
     * @param document 文档实体
     * @param chunks 文档分片
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rebuildDocumentIndex(AiKnowledgeDocument document, List<AiKnowledgeChunk> chunks) {
        long startedAt = System.currentTimeMillis();
        List<String> previousVectorIds = loadExistingVectorIds(document.getId());
        if (!previousVectorIds.isEmpty()) {
            vectorStore.delete(previousVectorIds);
        }

        if (chunks == null || chunks.isEmpty()) {
            log.info("AI 知识向量索引跳过: documentId={}, title={}, reason=no_chunks", document.getId(), document.getTitle());
            return;
        }

        List<Document> vectorDocuments = new ArrayList<>();
        for (AiKnowledgeChunk chunk : chunks) {
            String vectorId = buildVectorId(chunk.getId());
            chunk.setVectorId(vectorId);
            chunk.setEmbeddingStatus(0);
            aiKnowledgeChunkMapper.updateById(chunk);
            vectorDocuments.add(buildVectorDocument(document, chunk, vectorId));
        }

        try {
            vectorStore.add(vectorDocuments);
            for (AiKnowledgeChunk chunk : chunks) {
                chunk.setEmbeddingStatus(1);
                aiKnowledgeChunkMapper.updateById(chunk);
            }
            log.info(
                    "AI 知识向量索引完成: documentId={}, title={}, chunkCount={}, previousVectorCount={}, latencyMs={}",
                    document.getId(),
                    document.getTitle(),
                    chunks.size(),
                    previousVectorIds.size(),
                    System.currentTimeMillis() - startedAt
            );
        } catch (Exception exception) {
            log.error(
                    "AI 知识向量索引构建失败: documentId={}, title={}, chunkCount={}, latencyMs={}",
                    document.getId(),
                    document.getTitle(),
                    chunks.size(),
                    System.currentTimeMillis() - startedAt,
                    exception
            );
            for (AiKnowledgeChunk chunk : chunks) {
                chunk.setEmbeddingStatus(2);
                aiKnowledgeChunkMapper.updateById(chunk);
            }
            throw exception;
        }
    }

    /**
     * 清空当前 AI 知识向量数据，并将数据库中的向量状态重置为待处理。
     *
     * @return 清理的向量数量
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int clearAllVectorData() {
        List<AiKnowledgeChunk> chunks = aiKnowledgeChunkMapper.selectList(new LambdaQueryWrapper<AiKnowledgeChunk>()
                .isNotNull(AiKnowledgeChunk::getId));
        if (chunks == null || chunks.isEmpty()) {
            return 0;
        }

        List<String> vectorIds = new ArrayList<>();
        for (AiKnowledgeChunk chunk : chunks) {
            vectorIds.add(buildVectorId(chunk.getId()));
        }
        try {
            vectorStore.delete(vectorIds);
        } catch (Exception exception) {
            // 兼容 Redis 已被手动清空或索引缺失的场景，仍继续重置数据库状态。
            log.warn("AI 知识向量数据清理时未能删除 Redis 中的旧向量，继续重置数据库状态", exception);
        }
        aiKnowledgeChunkMapper.update(
                null,
                new LambdaUpdateWrapper<AiKnowledgeChunk>()
                        .set(AiKnowledgeChunk::getEmbeddingStatus, 0)
                        .set(AiKnowledgeChunk::getVectorId, "")
        );
        log.info("AI 知识向量数据清理完成: clearedVectorCount={}", vectorIds.size());
        return vectorIds.size();
    }

    /**
     * 读取指定文档当前已存在的向量 ID，便于重建前先删除旧索引。
     *
     * @param documentId 文档 ID
     * @return 已存在的向量 ID 列表
     */
    private List<String> loadExistingVectorIds(Long documentId) {
        List<AiKnowledgeChunk> chunks = aiKnowledgeChunkMapper.selectList(new LambdaQueryWrapper<AiKnowledgeChunk>()
                .eq(AiKnowledgeChunk::getDocumentId, documentId)
                .isNotNull(AiKnowledgeChunk::getVectorId));
        List<String> vectorIds = new ArrayList<>();
        for (AiKnowledgeChunk chunk : chunks) {
            if (StringUtils.hasText(chunk.getVectorId())) {
                vectorIds.add(chunk.getVectorId());
            }
        }
        return vectorIds;
    }

    /**
     * 将知识分片组装成可写入向量库的文档对象。
     *
     * @param document 文档实体
     * @param chunk 分片实体
     * @param vectorId 向量 ID
     * @return 向量文档
     */
    private Document buildVectorDocument(AiKnowledgeDocument document, AiKnowledgeChunk chunk, String vectorId) {
        return Document.builder()
                .id(vectorId)
                .text(chunk.getChunkText())
                .metadata(Map.of(
                        "documentId", document.getId(),
                        "chunkId", chunk.getId(),
                        "knowledgeDomain", document.getKnowledgeDomain(),
                        "title", normalize(document.getTitle()),
                        "sourceType", normalize(document.getSourceType()),
                        "sourceRef", normalize(document.getSourceRef())
                ))
                .build();
    }

    /**
     * 生成分片对应的向量主键。
     *
     * @param chunkId 分片 ID
     * @return 向量主键
     */
    private String buildVectorId(Long chunkId) {
        return VECTOR_ID_PREFIX + chunkId;
    }

    /**
     * 规范化元数据文本字段，避免向量库写入空白值。
     *
     * @param value 原始文本
     * @return 规范化结果
     */
    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : "";
    }
}

package com.travel.service.ai.rag;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.travel.common.exception.BusinessException;
import com.travel.common.result.ResultCode;
import com.travel.dto.ai.knowledge.AiKnowledgeDocumentDetailResponse;
import com.travel.dto.ai.knowledge.AiKnowledgeDocumentItem;
import com.travel.dto.ai.knowledge.AiKnowledgeMaintenanceResponse;
import com.travel.dto.ai.knowledge.AiKnowledgePreviewResponse;
import com.travel.dto.ai.knowledge.AiKnowledgeVectorIndexStatusResponse;
import com.travel.dto.ai.knowledge.ManualAiKnowledgeUpsertRequest;
import com.travel.entity.AiKnowledgeChunk;
import com.travel.entity.AiKnowledgeDocument;
import com.travel.enums.ai.AiScenarioType;
import com.travel.mapper.AiKnowledgeChunkMapper;
import com.travel.mapper.AiKnowledgeDocumentMapper;
import com.travel.service.ai.AiKnowledgeAdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.exceptions.JedisDataException;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * AI 知识管理服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiKnowledgeAdminServiceImpl implements AiKnowledgeAdminService {

    /**
     * 管理端统一时间格式。
     */
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 知识文档持久层。
     */
    private final AiKnowledgeDocumentMapper aiKnowledgeDocumentMapper;

    /**
     * 知识分片持久层。
     */
    private final AiKnowledgeChunkMapper aiKnowledgeChunkMapper;

    /**
     * 知识导入与分片重建服务。
     */
    private final AiKnowledgeIngestionService aiKnowledgeIngestionService;

    /**
     * 知识检索服务。
     */
    private final AiKnowledgeRetrievalService aiKnowledgeRetrievalService;

    /**
     * 向量索引维护服务。
     */
    private final AiKnowledgeVectorIndexService aiKnowledgeVectorIndexService;

    /**
     * 环境配置访问入口。
     */
    private final Environment environment;

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

    @Override
    public Long createManualDocument(ManualAiKnowledgeUpsertRequest request, Long adminId) {
        Long documentId = aiKnowledgeIngestionService.upsertManualDocument(request);
        log.info("管理端创建 AI 知识文档：adminId={}, documentId={}, title={}", adminId, documentId, request.getTitle());
        return documentId;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateManualDocument(Long documentId, ManualAiKnowledgeUpsertRequest request, Long adminId) {
        AiKnowledgeDocument document = getActiveDocument(documentId);
        document.setTitle(request.getTitle().trim());
        document.setKnowledgeDomain(request.getKnowledgeDomain().name());
        document.setContent(request.getContent().trim());
        document.setSourceType(normalize(request.getSourceType(), "manual"));
        document.setSourceRef(normalize(request.getSourceRef(), ""));
        document.setTags(normalize(request.getTags(), ""));
        document.setVersion(document.getVersion() == null ? 1 : document.getVersion() + 1);
        aiKnowledgeDocumentMapper.updateById(document);
        aiKnowledgeIngestionService.rebuildChunks(documentId);
        log.info("管理端更新 AI 知识文档：adminId={}, documentId={}, title={}", adminId, documentId, request.getTitle());
    }

    @Override
    public void rebuildDocumentChunks(Long documentId) {
        aiKnowledgeIngestionService.rebuildChunks(documentId);
    }

    @Override
    public List<AiKnowledgeDocumentItem> listDocuments() {
        List<AiKnowledgeDocument> documents = aiKnowledgeDocumentMapper.selectList(
                new LambdaQueryWrapper<AiKnowledgeDocument>()
                        .eq(AiKnowledgeDocument::getIsDeleted, 0)
                        .orderByDesc(AiKnowledgeDocument::getUpdatedAt)
        );
        if (documents == null || documents.isEmpty()) {
            return List.of();
        }
        return documents.stream().map(document -> {
            AiKnowledgeDocumentItem item = new AiKnowledgeDocumentItem();
            item.setId(document.getId());
            item.setTitle(document.getTitle());
            item.setKnowledgeDomain(document.getKnowledgeDomain());
            item.setSourceType(document.getSourceType());
            item.setSourceRef(document.getSourceRef());
            item.setVersion(document.getVersion());
            item.setIsEnabled(document.getIsEnabled());
            item.setChunkCount(countChunks(document.getId()));
            item.setUpdatedAt(document.getUpdatedAt() == null ? null : document.getUpdatedAt().format(DATETIME_FORMATTER));
            return item;
        }).toList();
    }

    @Override
    public AiKnowledgeDocumentDetailResponse getDocumentDetail(Long documentId) {
        AiKnowledgeDocument document = getActiveDocument(documentId);
        List<AiKnowledgeChunk> chunks = aiKnowledgeChunkMapper.selectList(
                new LambdaQueryWrapper<AiKnowledgeChunk>()
                        .eq(AiKnowledgeChunk::getDocumentId, documentId)
                        .orderByAsc(AiKnowledgeChunk::getChunkIndex)
        );

        AiKnowledgeDocumentDetailResponse response = new AiKnowledgeDocumentDetailResponse();
        response.setId(document.getId());
        response.setTitle(document.getTitle());
        response.setKnowledgeDomain(document.getKnowledgeDomain());
        response.setSourceType(document.getSourceType());
        response.setSourceRef(document.getSourceRef());
        response.setContent(document.getContent());
        response.setTags(document.getTags());
        response.setVersion(document.getVersion());
        response.setIsEnabled(document.getIsEnabled());
        response.setChunkCount(chunks.size());
        response.setUpdatedAt(document.getUpdatedAt() == null ? null : document.getUpdatedAt().format(DATETIME_FORMATTER));
        response.setChunks(chunks.stream().map(chunk -> new AiKnowledgeSnippet(
                document.getId(),
                chunk.getId(),
                document.getTitle(),
                document.getSourceType(),
                document.getSourceRef(),
                chunk.getChunkText(),
                document.getKnowledgeDomain()
        )).toList());
        return response;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateEnabledStatus(Long documentId, Integer isEnabled, Long adminId) {
        AiKnowledgeDocument document = getActiveDocument(documentId);
        document.setIsEnabled(isEnabled);
        aiKnowledgeDocumentMapper.updateById(document);
        log.info("管理端更新 AI 知识启用状态：adminId={}, documentId={}, isEnabled={}", adminId, documentId, isEnabled);
    }

    @Override
    public AiKnowledgePreviewResponse preview(AiScenarioType scenario, String query) {
        List<AiKnowledgeSnippet> hits = aiKnowledgeRetrievalService.retrieve(scenario, query);
        AiKnowledgePreviewResponse response = new AiKnowledgePreviewResponse();
        response.setQuery(query);
        response.setScenario(scenario.name());
        response.setDomain(aiKnowledgeRetrievalService.resolveDomain(scenario).name());
        response.setHitCount(hits.size());
        response.setHits(hits);
        return response;
    }

    @Override
    public AiKnowledgeVectorIndexStatusResponse getVectorIndexStatus() {
        AiKnowledgeVectorIndexStatusResponse response = new AiKnowledgeVectorIndexStatusResponse();
        response.setRagEnabled(Boolean.TRUE.equals(environment.getProperty("app.ai.rag.enabled", Boolean.class, Boolean.FALSE)));
        response.setChatProvider(environment.getProperty("spring.ai.model.chat", "ollama"));
        response.setEmbeddingProvider(environment.getProperty("spring.ai.model.embedding", "ollama"));
        response.setMixedProviderMode(!response.getChatProvider().equalsIgnoreCase(response.getEmbeddingProvider()));
        response.setChatModel(environment.getProperty("app.ai.provider.chat-model", ""));
        response.setEmbeddingModel(environment.getProperty("app.ai.provider.embedding-model", ""));
        response.setRedisHost(environment.getProperty("app.ai.vector.redis.host", "127.0.0.1"));
        response.setRedisPort(environment.getProperty("app.ai.vector.redis.port", Integer.class, 6379));
        response.setIndexName(environment.getProperty("app.ai.vector.redis.index-name", "waytrip-ai-knowledge-index"));
        response.setPrefix(environment.getProperty("app.ai.vector.redis.prefix", "waytrip:ai:chunk:"));
        response.setModelDimension(resolveModelDimension());
        response.setIndexDimension(resolveIndexDimension(response.getIndexName()));
        response.setDimensionMatched(
                response.getModelDimension() != null
                        && response.getIndexDimension() != null
                        && response.getModelDimension().intValue() == response.getIndexDimension().intValue()
        );
        response.setDocumentCount(countDocuments(null));
        response.setEnabledDocumentCount(countDocuments(1));
        response.setTotalChunkCount(countChunksByStatus(null));
        response.setPendingChunkCount(countChunksByStatus(0));
        response.setCompletedChunkCount(countChunksByStatus(1));
        response.setFailedChunkCount(countChunksByStatus(2));
        return response;
    }

    @Override
    public AiKnowledgeMaintenanceResponse clearVectorIndex() {
        int clearedVectorCount = aiKnowledgeVectorIndexService.clearAllVectorData();
        AiKnowledgeMaintenanceResponse response = new AiKnowledgeMaintenanceResponse();
        response.setClearedVectorCount(clearedVectorCount);
        response.setRebuiltDocumentCount(0);
        response.setRebuiltChunkCount(0);
        response.setMessage("AI 知识向量数据清理完成");
        return response;
    }

    @Override
    public AiKnowledgeMaintenanceResponse rebuildAllKnowledge() {
        return rebuildAllKnowledgeInternal(false);
    }

    @Override
    public AiKnowledgeMaintenanceResponse clearAndRebuildAllKnowledge() {
        return rebuildAllKnowledgeInternal(true);
    }

    /**
     * 按需清理向量数据后，重建全部启用知识文档。
     *
     * @param clearVectorDataFirst 是否先清空向量索引
     * @return 维护结果摘要
     */
    private AiKnowledgeMaintenanceResponse rebuildAllKnowledgeInternal(boolean clearVectorDataFirst) {
        int clearedVectorCount = 0;
        if (clearVectorDataFirst) {
            clearedVectorCount = aiKnowledgeVectorIndexService.clearAllVectorData();
        }
        List<AiKnowledgeDocument> documents = aiKnowledgeDocumentMapper.selectList(
                new LambdaQueryWrapper<AiKnowledgeDocument>()
                        .eq(AiKnowledgeDocument::getIsDeleted, 0)
                        .eq(AiKnowledgeDocument::getIsEnabled, 1)
        );
        int rebuilt = 0;
        if (documents != null) {
            for (AiKnowledgeDocument document : documents) {
                aiKnowledgeIngestionService.rebuildChunks(document.getId());
                rebuilt++;
            }
        }
        AiKnowledgeMaintenanceResponse response = new AiKnowledgeMaintenanceResponse();
        response.setClearedVectorCount(clearedVectorCount);
        response.setRebuiltDocumentCount(rebuilt);
        response.setRebuiltChunkCount(countEnabledDocumentChunks());
        response.setMessage(clearVectorDataFirst ? "AI 知识向量已清理并重建完成" : "AI 知识分片重建完成");
        return response;
    }

    /**
     * 获取当前仍然有效的知识文档。
     *
     * @param documentId 文档 ID
     * @return 文档实体
     */
    private AiKnowledgeDocument getActiveDocument(Long documentId) {
        AiKnowledgeDocument document = aiKnowledgeDocumentMapper.selectById(documentId);
        if (document == null || Integer.valueOf(1).equals(document.getIsDeleted())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "知识文档不存在");
        }
        return document;
    }

    /**
     * 统计指定文档的分片数量。
     *
     * @param documentId 文档 ID
     * @return 分片数量
     */
    private int countChunks(Long documentId) {
        Long count = aiKnowledgeChunkMapper.selectCount(
                new LambdaQueryWrapper<AiKnowledgeChunk>()
                        .eq(AiKnowledgeChunk::getDocumentId, documentId)
        );
        return count == null ? 0 : count.intValue();
    }

    /**
     * 统计文档数量，可按启用状态筛选。
     *
     * @param isEnabled 启用状态；为空表示统计全部
     * @return 文档数量
     */
    private int countDocuments(Integer isEnabled) {
        LambdaQueryWrapper<AiKnowledgeDocument> wrapper = new LambdaQueryWrapper<AiKnowledgeDocument>()
                .eq(AiKnowledgeDocument::getIsDeleted, 0);
        if (isEnabled != null) {
            wrapper.eq(AiKnowledgeDocument::getIsEnabled, isEnabled);
        }
        Long count = aiKnowledgeDocumentMapper.selectCount(wrapper);
        return count == null ? 0 : count.intValue();
    }

    /**
     * 按向量化状态统计分片数量。
     *
     * @param embeddingStatus 向量化状态；为空表示统计全部
     * @return 分片数量
     */
    private int countChunksByStatus(Integer embeddingStatus) {
        LambdaQueryWrapper<AiKnowledgeChunk> wrapper = new LambdaQueryWrapper<>();
        if (embeddingStatus != null) {
            wrapper.eq(AiKnowledgeChunk::getEmbeddingStatus, embeddingStatus);
        }
        Long count = aiKnowledgeChunkMapper.selectCount(wrapper);
        return count == null ? 0 : count.intValue();
    }

    /**
     * 统计所有启用知识文档的分片总量。
     *
     * @return 分片总数
     */
    private int countEnabledDocumentChunks() {
        List<AiKnowledgeDocument> enabledDocuments = aiKnowledgeDocumentMapper.selectList(
                new LambdaQueryWrapper<AiKnowledgeDocument>()
                        .eq(AiKnowledgeDocument::getIsDeleted, 0)
                        .eq(AiKnowledgeDocument::getIsEnabled, 1)
        );
        if (enabledDocuments == null || enabledDocuments.isEmpty()) {
            return 0;
        }
        int total = 0;
        for (AiKnowledgeDocument document : enabledDocuments) {
            total += countChunks(document.getId());
        }
        return total;
    }

    /**
     * 解析当前 embedding 模型维度。
     *
     * @return 模型维度；获取失败时返回 {@code null}
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

    /**
     * 对可选文本字段做裁剪和兜底，避免落库空白值。
     *
     * @param value 原始文本
     * @param fallback 兜底值
     * @return 规范化结果
     */
    private String normalize(String value, String fallback) {
        return org.springframework.util.StringUtils.hasText(value) ? value.trim() : fallback;
    }
}

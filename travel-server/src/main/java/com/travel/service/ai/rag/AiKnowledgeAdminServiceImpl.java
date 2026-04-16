package com.travel.service.ai.rag;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.travel.common.exception.BusinessException;
import com.travel.common.result.ResultCode;
import com.travel.dto.ai.knowledge.AiKnowledgeDocumentDetailResponse;
import com.travel.dto.ai.knowledge.AiKnowledgeDocumentItem;
import com.travel.dto.ai.knowledge.AiKnowledgePreviewResponse;
import com.travel.dto.ai.knowledge.ManualAiKnowledgeUpsertRequest;
import com.travel.entity.AiKnowledgeChunk;
import com.travel.entity.AiKnowledgeDocument;
import com.travel.enums.ai.AiScenarioType;
import com.travel.mapper.AiKnowledgeChunkMapper;
import com.travel.mapper.AiKnowledgeDocumentMapper;
import com.travel.service.ai.AiKnowledgeAdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 知识管理服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiKnowledgeAdminServiceImpl implements AiKnowledgeAdminService {

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final AiKnowledgeDocumentMapper aiKnowledgeDocumentMapper;
    private final AiKnowledgeChunkMapper aiKnowledgeChunkMapper;
    private final AiKnowledgeIngestionService aiKnowledgeIngestionService;
    private final AiKnowledgeRetrievalService aiKnowledgeRetrievalService;

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
    public Map<String, Object> rebuildAllKnowledge() {
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
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("documentCount", rebuilt);
        result.put("message", "AI 知识分片重建完成");
        return result;
    }

    private AiKnowledgeDocument getActiveDocument(Long documentId) {
        AiKnowledgeDocument document = aiKnowledgeDocumentMapper.selectById(documentId);
        if (document == null || Integer.valueOf(1).equals(document.getIsDeleted())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "知识文档不存在");
        }
        return document;
    }

    private int countChunks(Long documentId) {
        Long count = aiKnowledgeChunkMapper.selectCount(
                new LambdaQueryWrapper<AiKnowledgeChunk>()
                        .eq(AiKnowledgeChunk::getDocumentId, documentId)
        );
        return count == null ? 0 : count.intValue();
    }

    private String normalize(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }
}

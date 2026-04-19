package com.travel.service.ai.rag;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.travel.dto.ai.knowledge.ManualAiKnowledgeUpsertRequest;
import com.travel.entity.AiKnowledgeChunk;
import com.travel.entity.AiKnowledgeDocument;
import com.travel.enums.ai.AiKnowledgeIndexStatus;
import com.travel.mapper.AiKnowledgeChunkMapper;
import com.travel.mapper.AiKnowledgeDocumentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * AI 知识导入服务实现。
 * <p>
 * 管理 AI 知识文档落库以及分片构建。
 */
@Service
@RequiredArgsConstructor
public class AiKnowledgeIngestionServiceImpl implements AiKnowledgeIngestionService {

    private final AiKnowledgeDocumentMapper aiKnowledgeDocumentMapper;
    private final AiKnowledgeChunkMapper aiKnowledgeChunkMapper;
    private final AiKnowledgeVectorIndexService aiKnowledgeVectorIndexService;

    @Override
    public Long upsertManualDocument(ManualAiKnowledgeUpsertRequest request) {
        AiKnowledgeDocument document = new AiKnowledgeDocument();
        document.setTitle(request.getTitle().trim());
        document.setKnowledgeDomain(request.getKnowledgeDomain().name());
        document.setSourceType(normalize(request.getSourceType(), "manual"));
        document.setSourceRef(normalize(request.getSourceRef(), ""));
        document.setContent(request.getContent().trim());
        document.setTags(normalize(request.getTags(), ""));
        document.setVersion(1);
        document.setIndexStatus(AiKnowledgeIndexStatus.PENDING.name());
        document.setRetryCount(0);
        document.setLastError("");
        document.setIsEnabled(1);
        document.setIsDeleted(0);
        aiKnowledgeDocumentMapper.insert(document);
        return document.getId();
    }

    @Override
    public void rebuildChunks(Long documentId) {
        processDocumentChunks(documentId);
    }

    /**
     * 同步执行单个文档的分片与向量构建。
     *
     * @param documentId 文档 ID
     */
    public void processDocumentChunks(Long documentId) {
        AiKnowledgeDocument document = aiKnowledgeDocumentMapper.selectById(documentId);
        if (document == null || document.getIsDeleted() != null && document.getIsDeleted() == 1) {
            return;
        }
        aiKnowledgeChunkMapper.delete(new LambdaQueryWrapper<AiKnowledgeChunk>()
                .eq(AiKnowledgeChunk::getDocumentId, documentId));
        List<String> segments = splitToSegments(document.getContent());
        List<AiKnowledgeChunk> chunks = new ArrayList<>();
        for (int i = 0; i < segments.size(); i++) {
            AiKnowledgeChunk chunk = new AiKnowledgeChunk();
            chunk.setDocumentId(documentId);
            chunk.setChunkIndex(i + 1);
            chunk.setChunkText(segments.get(i));
            chunk.setChunkSummary(buildSummary(segments.get(i)));
            chunk.setEmbeddingStatus(0);
            chunk.setVectorId("");
            aiKnowledgeChunkMapper.insert(chunk);
            chunks.add(chunk);
        }
        aiKnowledgeVectorIndexService.rebuildDocumentIndex(document, chunks);
    }

    private List<String> splitToSegments(String content) {
        List<String> segments = new ArrayList<>();
        if (!StringUtils.hasText(content)) {
            return segments;
        }
        String[] blocks = content.trim().split("(\\r?\\n){2,}");
        for (String block : blocks) {
            String normalized = block.trim();
            if (!StringUtils.hasText(normalized)) {
                continue;
            }
            if (normalized.length() <= 220) {
                segments.add(normalized);
                continue;
            }
            int start = 0;
            while (start < normalized.length()) {
                int end = Math.min(start + 220, normalized.length());
                segments.add(normalized.substring(start, end));
                start = end;
            }
        }
        return segments;
    }

    private String buildSummary(String content) {
        if (!StringUtils.hasText(content)) {
            return "";
        }
        String trimmed = content.trim();
        return trimmed.length() <= 80 ? trimmed : trimmed.substring(0, 80);
    }

    private String normalize(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }
}

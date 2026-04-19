package com.travel.service.ai.rag;

import com.travel.entity.AiKnowledgeDocument;
import com.travel.enums.ai.AiKnowledgeIndexStatus;
import com.travel.mapper.AiKnowledgeDocumentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * AI 知识任务生产者。
 */
@Service
@RequiredArgsConstructor
public class AiKnowledgeImportJobServiceImpl implements AiKnowledgeImportJobService {

    /**
     * Redis 字符串模板。
     */
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 知识文档持久层。
     */
    private final AiKnowledgeDocumentMapper aiKnowledgeDocumentMapper;

    @Override
    public void enqueueDocumentRebuild(Long documentId) {
        markQueued(documentId);
        stringRedisTemplate.opsForStream().add(
                AiKnowledgeJobStreamSupport.STREAM_KEY,
                Map.of(
                        AiKnowledgeJobStreamSupport.FIELD_TYPE, AiKnowledgeJobStreamSupport.TYPE_REBUILD_DOCUMENT,
                        AiKnowledgeJobStreamSupport.FIELD_DOCUMENT_ID, String.valueOf(documentId)
                )
        );
    }

    /**
     * 标记文档任务已入队。
     *
     * @param documentId 文档 ID
     */
    private void markQueued(Long documentId) {
        AiKnowledgeDocument document = aiKnowledgeDocumentMapper.selectById(documentId);
        if (document == null || Integer.valueOf(1).equals(document.getIsDeleted())) {
            return;
        }
        document.setIndexStatus(AiKnowledgeIndexStatus.PENDING.name());
        document.setLastError("");
        document.setRebuildRequestedAt(LocalDateTime.now());
        aiKnowledgeDocumentMapper.updateById(document);
    }
}

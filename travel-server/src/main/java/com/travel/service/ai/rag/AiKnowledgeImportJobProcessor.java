package com.travel.service.ai.rag;

import com.travel.entity.AiKnowledgeDocument;
import com.travel.enums.ai.AiKnowledgeIndexStatus;
import com.travel.mapper.AiKnowledgeDocumentMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AI 知识任务处理器。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiKnowledgeImportJobProcessor {

    /**
     * 知识导入服务实现。
     */
    private final AiKnowledgeIngestionServiceImpl aiKnowledgeIngestionService;

    /**
     * 知识文档持久层。
     */
    private final AiKnowledgeDocumentMapper aiKnowledgeDocumentMapper;

    /**
     * 当前正在执行的文档任务集合。
     */
    private final Set<Long> runningDocumentIds = ConcurrentHashMap.newKeySet();

    /**
     * 需要在当前任务完成后立即补跑的文档集合。
     */
    private final Set<Long> rerunDocumentIds = ConcurrentHashMap.newKeySet();

    /**
     * 执行指定文档的后台重建任务。
     *
     * @param documentId 文档 ID
     */
    public void processDocument(Long documentId) {
        if (!runningDocumentIds.add(documentId)) {
            rerunDocumentIds.add(documentId);
            log.info("AI 知识后台任务已在执行，标记补跑：documentId={}", documentId);
            return;
        }
        try {
            markProcessing(documentId);
            aiKnowledgeIngestionService.processDocumentChunks(documentId);
            markSuccess(documentId);
            log.info("AI 知识后台任务完成：documentId={}", documentId);
        } catch (Exception exception) {
            markFailed(documentId, exception);
            log.error("AI 知识后台任务失败：documentId={}", documentId, exception);
        } finally {
            runningDocumentIds.remove(documentId);
            if (rerunDocumentIds.remove(documentId)) {
                processDocument(documentId);
            }
        }
    }

    private void markProcessing(Long documentId) {
        AiKnowledgeDocument document = aiKnowledgeDocumentMapper.selectById(documentId);
        if (document == null || Integer.valueOf(1).equals(document.getIsDeleted())) {
            return;
        }
        document.setIndexStatus(AiKnowledgeIndexStatus.PROCESSING.name());
        document.setRebuildStartedAt(LocalDateTime.now());
        document.setRebuildFinishedAt(null);
        aiKnowledgeDocumentMapper.updateById(document);
    }

    private void markSuccess(Long documentId) {
        AiKnowledgeDocument document = aiKnowledgeDocumentMapper.selectById(documentId);
        if (document == null || Integer.valueOf(1).equals(document.getIsDeleted())) {
            return;
        }
        document.setIndexStatus(AiKnowledgeIndexStatus.SUCCESS.name());
        document.setLastError("");
        document.setRebuildFinishedAt(LocalDateTime.now());
        aiKnowledgeDocumentMapper.updateById(document);
    }

    private void markFailed(Long documentId, Exception exception) {
        AiKnowledgeDocument document = aiKnowledgeDocumentMapper.selectById(documentId);
        if (document == null || Integer.valueOf(1).equals(document.getIsDeleted())) {
            return;
        }
        document.setIndexStatus(AiKnowledgeIndexStatus.FAILED.name());
        document.setRetryCount((document.getRetryCount() == null ? 0 : document.getRetryCount()) + 1);
        document.setLastError(truncateError(exception == null ? "" : exception.getMessage()));
        document.setRebuildFinishedAt(LocalDateTime.now());
        aiKnowledgeDocumentMapper.updateById(document);
    }

    private String truncateError(String message) {
        if (message == null || message.isBlank()) {
            return "后台任务执行失败";
        }
        return message.length() <= 512 ? message : message.substring(0, 512);
    }
}

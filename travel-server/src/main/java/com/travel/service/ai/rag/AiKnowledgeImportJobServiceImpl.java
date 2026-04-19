package com.travel.service.ai.rag;

import com.travel.entity.AiKnowledgeDocument;
import com.travel.enums.ai.AiKnowledgeIndexStatus;
import com.travel.mapper.AiKnowledgeDocumentMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AI 知识导入任务服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiKnowledgeImportJobServiceImpl implements AiKnowledgeImportJobService {

    /**
     * AI 知识后台任务执行器。
     */
    @Qualifier("aiKnowledgeJobExecutor")
    private final TaskExecutor aiKnowledgeJobExecutor;

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

    @Override
    public void enqueueDocumentRebuild(Long documentId) {
        markQueued(documentId);
        if (!runningDocumentIds.add(documentId)) {
            rerunDocumentIds.add(documentId);
            log.info("AI 知识后台任务已在执行，标记补跑：documentId={}", documentId);
            return;
        }
        aiKnowledgeJobExecutor.execute(() -> {
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
                    enqueueDocumentRebuild(documentId);
                }
            }
        });
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

    /**
     * 标记文档任务开始执行。
     *
     * @param documentId 文档 ID
     */
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

    /**
     * 标记文档任务成功完成。
     *
     * @param documentId 文档 ID
     */
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

    /**
     * 标记文档任务执行失败。
     *
     * @param documentId 文档 ID
     * @param exception 异常
     */
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

    /**
     * 裁剪错误信息，避免异常栈摘要超出数据库字段长度。
     *
     * @param message 原始错误信息
     * @return 裁剪后的错误信息
     */
    private String truncateError(String message) {
        if (message == null || message.isBlank()) {
            return "后台任务执行失败";
        }
        return message.length() <= 512 ? message : message.substring(0, 512);
    }
}

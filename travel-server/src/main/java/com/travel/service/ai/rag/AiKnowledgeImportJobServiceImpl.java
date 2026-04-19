package com.travel.service.ai.rag;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;

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

    @Override
    public void enqueueDocumentRebuild(Long documentId) {
        aiKnowledgeJobExecutor.execute(() -> {
            try {
                aiKnowledgeIngestionService.processDocumentChunks(documentId);
                log.info("AI 知识后台任务完成：documentId={}", documentId);
            } catch (Exception exception) {
                log.error("AI 知识后台任务失败：documentId={}", documentId, exception);
            }
        });
    }
}

package com.travel.service.ai.rag;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.travel.entity.AiKnowledgeDocument;
import com.travel.enums.ai.AiKnowledgeIndexStatus;
import com.travel.mapper.AiKnowledgeDocumentMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * AI 知识任务恢复器。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiKnowledgeJobRecovery {

    private final AiKnowledgeDocumentMapper aiKnowledgeDocumentMapper;
    private final AiKnowledgeImportJobService aiKnowledgeImportJobService;

    /**
     * 应用启动后恢复中断的知识任务。
     */
    @EventListener(ApplicationReadyEvent.class)
    public void recoverPendingDocuments() {
        List<AiKnowledgeDocument> documents = aiKnowledgeDocumentMapper.selectList(
                new LambdaQueryWrapper<AiKnowledgeDocument>()
                        .eq(AiKnowledgeDocument::getIsDeleted, 0)
                        .in(AiKnowledgeDocument::getIndexStatus,
                                AiKnowledgeIndexStatus.PENDING.name(),
                                AiKnowledgeIndexStatus.PROCESSING.name())
        );
        if (documents == null || documents.isEmpty()) {
            return;
        }
        for (AiKnowledgeDocument document : documents) {
            aiKnowledgeImportJobService.enqueueDocumentRebuild(document.getId());
        }
        log.info("AI 知识任务恢复完成：queuedDocumentCount={}", documents.size());
    }
}

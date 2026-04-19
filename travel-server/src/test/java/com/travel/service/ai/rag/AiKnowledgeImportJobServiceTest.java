package com.travel.service.ai.rag;

import com.travel.entity.AiKnowledgeDocument;
import com.travel.enums.ai.AiKnowledgeIndexStatus;
import com.travel.mapper.AiKnowledgeDocumentMapper;
import org.junit.jupiter.api.Test;
import org.springframework.core.task.TaskExecutor;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * AI 知识后台任务测试。
 */
class AiKnowledgeImportJobServiceTest {

    @Test
    void enqueueDocumentRebuildUpdatesDocumentStatus() {
        TaskExecutor executor = Runnable::run;
        AiKnowledgeIngestionServiceImpl ingestionService = mock(AiKnowledgeIngestionServiceImpl.class);
        AiKnowledgeDocumentMapper documentMapper = mock(AiKnowledgeDocumentMapper.class);

        AiKnowledgeDocument document = new AiKnowledgeDocument();
        document.setId(1L);
        document.setIsDeleted(0);
        document.setIndexStatus(AiKnowledgeIndexStatus.PENDING.name());
        document.setRetryCount(0);

        when(documentMapper.selectById(anyLong())).thenReturn(document);
        List<String> statuses = new ArrayList<>();
        doAnswer(invocation -> {
            AiKnowledgeDocument updated = invocation.getArgument(0);
            statuses.add(updated.getIndexStatus());
            return 1;
        }).when(documentMapper).updateById(org.mockito.ArgumentMatchers.any(AiKnowledgeDocument.class));

        AiKnowledgeImportJobServiceImpl service =
                new AiKnowledgeImportJobServiceImpl(executor, ingestionService, documentMapper);

        service.enqueueDocumentRebuild(1L);

        verify(ingestionService).processDocumentChunks(1L);
        assertTrue(statuses.contains(AiKnowledgeIndexStatus.PENDING.name()));
        assertTrue(statuses.contains(AiKnowledgeIndexStatus.PROCESSING.name()));
        assertTrue(statuses.contains(AiKnowledgeIndexStatus.SUCCESS.name()));
    }
}

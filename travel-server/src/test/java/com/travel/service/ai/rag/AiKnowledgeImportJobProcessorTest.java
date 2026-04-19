package com.travel.service.ai.rag;

import com.travel.entity.AiKnowledgeDocument;
import com.travel.enums.ai.AiKnowledgeIndexStatus;
import com.travel.mapper.AiKnowledgeDocumentMapper;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * AI 知识任务处理器测试。
 */
class AiKnowledgeImportJobProcessorTest {

    @Test
    void processDocumentUpdatesDocumentStatus() {
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

        AiKnowledgeImportJobProcessor processor = new AiKnowledgeImportJobProcessor(ingestionService, documentMapper);

        processor.processDocument(1L);

        verify(ingestionService).processDocumentChunks(1L);
        assertTrue(statuses.contains(AiKnowledgeIndexStatus.PROCESSING.name()));
        assertTrue(statuses.contains(AiKnowledgeIndexStatus.SUCCESS.name()));
    }
}

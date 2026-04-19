package com.travel.controller.admin;

import com.travel.common.exception.GlobalExceptionHandler;
import com.travel.dto.ai.knowledge.AiKnowledgeDocumentDetailResponse;
import com.travel.dto.ai.knowledge.AiKnowledgeJobResponse;
import com.travel.dto.ai.knowledge.ManualAiKnowledgeUpsertRequest;
import com.travel.service.ai.AiKnowledgeAdminService;
import com.travel.util.web.UserContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 管理端 AI 知识控制器测试。
 */
@WebMvcTest(AdminAiKnowledgeController.class)
@Import(GlobalExceptionHandler.class)
class AdminAiKnowledgeControllerTest {

    private static final Long ADMIN_ID = 1001L;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AiKnowledgeAdminService aiKnowledgeAdminService;

    @AfterEach
    void tearDown() {
        UserContextHolder.clear();
    }

    @Test
    void shouldReturnDocumentDetail() throws Exception {
        AiKnowledgeDocumentDetailResponse response = new AiKnowledgeDocumentDetailResponse();
        response.setId(1L);
        response.setTitle("平台订单售后说明");
        response.setKnowledgeDomain("PLATFORM_POLICY");
        response.setContent("订单退款、改签、取消等售后问题...");
        response.setIsEnabled(1);
        response.setChunkCount(2);

        when(aiKnowledgeAdminService.getDocumentDetail(1L)).thenReturn(response);

        mockMvc.perform(get("/api/admin/v1/ai/knowledge/documents/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.title").value("平台订单售后说明"))
                .andExpect(jsonPath("$.data.chunkCount").value(2));
    }

    @Test
    void shouldCreateManualDocument() throws Exception {
        UserContextHolder.setAdminId(ADMIN_ID);

        AiKnowledgeJobResponse response = new AiKnowledgeJobResponse();
        response.setDocumentId(2L);
        response.setIndexStatus("PENDING");
        response.setQueuedDocumentCount(1);
        response.setMessage("AI 知识文档已创建，索引任务已入队");
        when(aiKnowledgeAdminService.createManualDocument(any(ManualAiKnowledgeUpsertRequest.class), eq(ADMIN_ID))).thenReturn(response);

        mockMvc.perform(post("/api/admin/v1/ai/knowledge/documents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  \"title\": \"旅游规划回答规则\",
                                  \"knowledgeDomain\": \"SPOT_KNOWLEDGE\",
                                  \"content\": \"先给初步建议，再提示可补充预算和时间。\",
                                  \"sourceType\": \"manual\",
                                  \"sourceRef\": \"policy:trip-planning\",
                                  \"tags\": \"推荐,规划\"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.documentId").value(2))
                .andExpect(jsonPath("$.data.indexStatus").value("PENDING"));
    }

    @Test
    void shouldUpdateManualDocument() throws Exception {
        UserContextHolder.setAdminId(ADMIN_ID);
        AiKnowledgeJobResponse response = new AiKnowledgeJobResponse();
        response.setDocumentId(1L);
        response.setIndexStatus("PENDING");
        response.setQueuedDocumentCount(1);
        response.setMessage("AI 知识文档已更新，索引任务已入队");
        when(aiKnowledgeAdminService.updateManualDocument(eq(1L), any(ManualAiKnowledgeUpsertRequest.class), eq(ADMIN_ID))).thenReturn(response);

        mockMvc.perform(put("/api/admin/v1/ai/knowledge/documents/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  \"title\": \"平台订单售后说明-更新\",
                                  \"knowledgeDomain\": \"PLATFORM_POLICY\",
                                  \"content\": \"更新后的知识正文\",
                                  \"sourceType\": \"manual\",
                                  \"sourceRef\": \"policy:order-after-sale\",
                                  \"tags\": \"订单,售后\"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.documentId").value(1))
                .andExpect(jsonPath("$.data.indexStatus").value("PENDING"));

        verify(aiKnowledgeAdminService).updateManualDocument(eq(1L), any(ManualAiKnowledgeUpsertRequest.class), eq(ADMIN_ID));
    }

    @Test
    void shouldRetryFailedDocumentRebuild() throws Exception {
        AiKnowledgeJobResponse response = new AiKnowledgeJobResponse();
        response.setDocumentId(1L);
        response.setIndexStatus("PENDING");
        response.setQueuedDocumentCount(1);
        response.setMessage("AI 知识失败任务已重新入队");
        when(aiKnowledgeAdminService.retryFailedDocumentRebuild(1L)).thenReturn(response);

        mockMvc.perform(post("/api/admin/v1/ai/knowledge/documents/1/retry"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.documentId").value(1))
                .andExpect(jsonPath("$.data.indexStatus").value("PENDING"));
    }

    @Test
    void shouldUpdateDocumentEnabledStatus() throws Exception {
        UserContextHolder.setAdminId(ADMIN_ID);

        mockMvc.perform(put("/api/admin/v1/ai/knowledge/documents/1/enabled")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  \"isEnabled\": 0
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        verify(aiKnowledgeAdminService).updateEnabledStatus(1L, 0, ADMIN_ID);
    }

    @Test
    void shouldRejectInvalidEnabledStatus() throws Exception {
        mockMvc.perform(put("/api/admin/v1/ai/knowledge/documents/1/enabled")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  \"isEnabled\": 2
                                }
                                """))
                .andExpect(status().is4xxClientError());
    }
}

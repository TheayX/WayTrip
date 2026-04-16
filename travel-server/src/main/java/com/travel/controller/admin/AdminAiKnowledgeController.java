package com.travel.controller.admin;

import com.travel.common.result.ApiResponse;
import com.travel.dto.ai.knowledge.AiKnowledgeDocumentDetailResponse;
import com.travel.dto.ai.knowledge.AiKnowledgeDocumentItem;
import com.travel.dto.ai.knowledge.AiKnowledgePreviewResponse;
import com.travel.dto.ai.knowledge.ManualAiKnowledgeUpsertRequest;
import com.travel.dto.ai.knowledge.UpdateAiKnowledgeEnabledRequest;
import com.travel.enums.ai.AiScenarioType;
import com.travel.service.ai.AiKnowledgeAdminService;
import com.travel.util.web.UserContextHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 管理端 AI 知识控制器。
 */
@Tag(name = "管理端-AI 知识", description = "管理端 AI 知识管理与 RAG 预览接口")
@RestController
@RequestMapping("/api/admin/v1/ai")
@RequiredArgsConstructor
@Slf4j
public class AdminAiKnowledgeController {

    private final AiKnowledgeAdminService aiKnowledgeAdminService;

    /**
     * 获取 AI 知识文档列表。
     *
     * @return 知识文档摘要
     */
    @Operation(summary = "获取 AI 知识文档列表")
    @GetMapping("/knowledge/documents")
    public ApiResponse<List<AiKnowledgeDocumentItem>> listDocuments() {
        return ApiResponse.success(aiKnowledgeAdminService.listDocuments());
    }

    /**
     * 获取 AI 知识文档详情。
     *
     * @param documentId 文档 ID
     * @return 文档详情
     */
    @Operation(summary = "获取 AI 知识文档详情")
    @GetMapping("/knowledge/documents/{documentId}")
    public ApiResponse<AiKnowledgeDocumentDetailResponse> getDocumentDetail(@PathVariable Long documentId) {
        return ApiResponse.success(aiKnowledgeAdminService.getDocumentDetail(documentId));
    }

    /**
     * 创建手工知识文档。
     *
     * @param request 导入请求
     * @return 新建文档 ID
     */
    @Operation(summary = "创建手工 AI 知识文档")
    @PostMapping("/knowledge/documents")
    public ApiResponse<Map<String, Long>> createManualDocument(@Valid @RequestBody ManualAiKnowledgeUpsertRequest request) {
        Long adminId = UserContextHolder.getAdminId();
        Long id = aiKnowledgeAdminService.createManualDocument(request, adminId);
        return ApiResponse.success(Map.of("id", id));
    }

    /**
     * 更新手工知识文档。
     *
     * @param documentId 文档 ID
     * @param request 更新请求
     * @return 空响应
     */
    @Operation(summary = "更新手工 AI 知识文档")
    @PutMapping("/knowledge/documents/{documentId}")
    public ApiResponse<Void> updateManualDocument(
            @PathVariable Long documentId,
            @Valid @RequestBody ManualAiKnowledgeUpsertRequest request) {
        aiKnowledgeAdminService.updateManualDocument(documentId, request, UserContextHolder.getAdminId());
        return ApiResponse.success();
    }

    /**
     * 更新 AI 知识文档启用状态。
     *
     * @param documentId 文档 ID
     * @param request 启用状态请求
     * @return 空响应
     */
    @Operation(summary = "更新 AI 知识文档启用状态")
    @PutMapping("/knowledge/documents/{documentId}/enabled")
    public ApiResponse<Void> updateEnabledStatus(
            @PathVariable Long documentId,
            @Valid @RequestBody UpdateAiKnowledgeEnabledRequest request) {
        aiKnowledgeAdminService.updateEnabledStatus(documentId, request.getIsEnabled(), UserContextHolder.getAdminId());
        return ApiResponse.success();
    }

    /**
     * 重建指定知识文档的分片。
     *
     * @param documentId 文档 ID
     * @return 空响应
     */
    @Operation(summary = "重建指定知识文档分片")
    @PutMapping("/knowledge/documents/{documentId}/rebuild")
    public ApiResponse<Void> rebuildDocumentChunks(@PathVariable Long documentId) {
        aiKnowledgeAdminService.rebuildDocumentChunks(documentId);
        return ApiResponse.success();
    }

    /**
     * 手动触发全部 AI 知识分片重建。
     *
     * @return 重建结果摘要
     */
    @Operation(summary = "重建全部 AI 知识分片")
    @PostMapping("/knowledge/rebuild")
    public ApiResponse<Map<String, Object>> rebuildAllKnowledge() {
        log.info("管理端触发 AI 知识分片重建：adminId={}", UserContextHolder.getAdminId());
        return ApiResponse.success(aiKnowledgeAdminService.rebuildAllKnowledge());
    }

    /**
     * 预览当前 RAG 检索命中结果。
     *
     * @param scenario 场景类型
     * @param query 查询语句
     * @return 检索预览结果
     */
    @Operation(summary = "预览 AI 知识检索结果")
    @GetMapping("/rag/preview")
    public ApiResponse<AiKnowledgePreviewResponse> preview(
            @RequestParam AiScenarioType scenario,
            @RequestParam String query) {
        return ApiResponse.success(aiKnowledgeAdminService.preview(scenario, query));
    }
}

package com.travel.service.ai;

import com.travel.dto.ai.knowledge.AiKnowledgeDocumentDetailResponse;
import com.travel.dto.ai.knowledge.AiKnowledgeDocumentItem;
import com.travel.dto.ai.knowledge.AiKnowledgePreviewResponse;
import com.travel.dto.ai.knowledge.ManualAiKnowledgeUpsertRequest;
import com.travel.enums.ai.AiScenarioType;

import java.util.List;
import java.util.Map;

/**
 * AI 知识管理服务接口。
 */
public interface AiKnowledgeAdminService {

    /**
     * 创建知识文档并构建分片。
     *
     * @param request 导入请求
     * @param adminId 当前管理员 ID
     * @return 文档 ID
     */
    Long createManualDocument(ManualAiKnowledgeUpsertRequest request, Long adminId);

    /**
     * 更新手工知识文档并重建分片。
     *
     * @param documentId 文档 ID
     * @param request 导入请求
     * @param adminId 当前管理员 ID
     */
    void updateManualDocument(Long documentId, ManualAiKnowledgeUpsertRequest request, Long adminId);

    /**
     * 重建指定知识文档的分片。
     *
     * @param documentId 文档 ID
     */
    void rebuildDocumentChunks(Long documentId);

    /**
     * 获取知识文档列表。
     *
     * @return 文档摘要集合
     */
    List<AiKnowledgeDocumentItem> listDocuments();

    /**
     * 获取知识文档详情。
     *
     * @param documentId 文档 ID
     * @return 文档详情
     */
    AiKnowledgeDocumentDetailResponse getDocumentDetail(Long documentId);

    /**
     * 更新知识文档启用状态。
     *
     * @param documentId 文档 ID
     * @param isEnabled 启用状态
     * @param adminId 当前管理员 ID
     */
    void updateEnabledStatus(Long documentId, Integer isEnabled, Long adminId);

    /**
     * 预览某个查询在当前 RAG 链路中的命中结果。
     *
     * @param scenario 场景类型
     * @param query 查询语句
     * @return 检索预览结果
     */
    AiKnowledgePreviewResponse preview(AiScenarioType scenario, String query);

    /**
     * 手动触发知识重建任务。
     *
     * @return 结果摘要
     */
    Map<String, Object> rebuildAllKnowledge();
}

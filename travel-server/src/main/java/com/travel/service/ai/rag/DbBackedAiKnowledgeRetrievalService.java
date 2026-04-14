package com.travel.service.ai.rag;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.travel.config.ai.AiProperties;
import com.travel.entity.AiKnowledgeChunk;
import com.travel.entity.AiKnowledgeDocument;
import com.travel.enums.ai.AiKnowledgeDomain;
import com.travel.enums.ai.AiScenarioType;
import com.travel.mapper.AiKnowledgeChunkMapper;
import com.travel.mapper.AiKnowledgeDocumentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 基于数据库分片的 AI 知识检索服务。
 * <p>
 * 第一阶段先提供稳定的知识骨架和引用回传能力，后续再切到向量检索。
 */
@Service
@RequiredArgsConstructor
public class DbBackedAiKnowledgeRetrievalService implements AiKnowledgeRetrievalService {

    private final AiProperties aiProperties;
    private final AiKnowledgeDocumentMapper aiKnowledgeDocumentMapper;
    private final AiKnowledgeChunkMapper aiKnowledgeChunkMapper;

    @Override
    public List<AiKnowledgeSnippet> retrieve(AiScenarioType scenario, String userMessage) {
        if (!Boolean.TRUE.equals(aiProperties.getRag().getEnabled()) || !StringUtils.hasText(userMessage)) {
            return List.of();
        }
        AiKnowledgeDomain domain = resolveDomain(scenario);
        List<String> keywords = extractKeywords(userMessage);
        List<AiKnowledgeDocument> documents = aiKnowledgeDocumentMapper.selectList(
                new LambdaQueryWrapper<AiKnowledgeDocument>()
                        .eq(AiKnowledgeDocument::getKnowledgeDomain, domain.name())
                        .eq(AiKnowledgeDocument::getIsEnabled, 1)
                        .eq(AiKnowledgeDocument::getIsDeleted, 0)
                        .orderByDesc(AiKnowledgeDocument::getUpdatedAt)
                        .last("LIMIT 20")
        );
        if (documents == null || documents.isEmpty()) {
            return List.of();
        }
        Map<Long, AiKnowledgeDocument> documentMap = new LinkedHashMap<>();
        for (AiKnowledgeDocument document : documents) {
            documentMap.put(document.getId(), document);
        }

        List<AiKnowledgeSnippet> matches = new ArrayList<>();
        for (AiKnowledgeDocument document : documents) {
            List<AiKnowledgeChunk> chunks = aiKnowledgeChunkMapper.selectList(
                    new LambdaQueryWrapper<AiKnowledgeChunk>()
                            .eq(AiKnowledgeChunk::getDocumentId, document.getId())
                            .orderByAsc(AiKnowledgeChunk::getChunkIndex)
            );
            if (chunks == null || chunks.isEmpty()) {
                continue;
            }
            for (AiKnowledgeChunk chunk : chunks) {
                if (!matchesKeyword(chunk, keywords)) {
                    continue;
                }
                matches.add(new AiKnowledgeSnippet(
                        document.getId(),
                        chunk.getId(),
                        document.getTitle(),
                        document.getSourceType(),
                        document.getSourceRef(),
                        truncateSnippet(chunk.getChunkText()),
                        document.getKnowledgeDomain()
                ));
                if (matches.size() >= Math.max(1, aiProperties.getRag().getTopK())) {
                    return matches;
                }
            }
        }
        return matches;
    }

    @Override
    public AiKnowledgeDomain resolveDomain(AiScenarioType scenario) {
        return switch (scenario) {
            case ORDER_ADVISOR -> AiKnowledgeDomain.PLATFORM_POLICY;
            case TRAVEL_PLANNER, RECOMMENDATION_EXPLAINER -> AiKnowledgeDomain.SPOT_KNOWLEDGE;
            case USER_PROFILE_ANALYZER -> AiKnowledgeDomain.ACCOUNT_HELP;
            case OPERATION_ANALYZER -> AiKnowledgeDomain.PLATFORM_POLICY;
            default -> AiKnowledgeDomain.PLATFORM_POLICY;
        };
    }

    private List<String> extractKeywords(String userMessage) {
        String normalized = userMessage.trim().replaceAll("[，。！？、,.!?]", " ");
        List<String> result = new ArrayList<>();
        for (String token : normalized.split("\\s+")) {
            if (token.length() >= 2) {
                result.add(token);
            }
        }
        if (result.isEmpty() && StringUtils.hasText(normalized)) {
            result.add(normalized);
        }
        return result;
    }

    private boolean matchesKeyword(AiKnowledgeChunk chunk, List<String> keywords) {
        String content = (chunk.getChunkText() == null ? "" : chunk.getChunkText())
                + " "
                + (chunk.getChunkSummary() == null ? "" : chunk.getChunkSummary());
        for (String keyword : keywords) {
            if (content.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private String truncateSnippet(String content) {
        if (!StringUtils.hasText(content)) {
            return "";
        }
        String trimmed = content.trim();
        return trimmed.length() <= 160 ? trimmed : trimmed.substring(0, 160);
    }
}

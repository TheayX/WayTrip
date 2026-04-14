package com.travel.service.ai.rag;

import com.travel.config.ai.AiProperties;
import com.travel.enums.ai.AiKnowledgeDomain;
import com.travel.enums.ai.AiScenarioType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 基于 Redis Vector Store 的 AI 知识检索服务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisVectorAiKnowledgeRetrievalService implements AiKnowledgeRetrievalService {

    private final AiProperties aiProperties;
    private final VectorStore vectorStore;

    @Override
    public List<AiKnowledgeSnippet> retrieve(AiScenarioType scenario, String userMessage) {
        if (!Boolean.TRUE.equals(aiProperties.getRag().getEnabled()) || !StringUtils.hasText(userMessage)) {
            return List.of();
        }

        AiKnowledgeDomain domain = resolveDomain(scenario);
        SearchRequest request = SearchRequest.builder()
                .query(userMessage.trim())
                .topK(resolveCandidateCount())
                .similarityThreshold(resolveSimilarityThreshold())
                .build();

        try {
            return mapToSnippets(vectorStore.similaritySearch(request), domain);
        } catch (Exception exception) {
            log.error("AI 向量检索失败, scenario={}", scenario, exception);
            return List.of();
        }
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

    private List<AiKnowledgeSnippet> mapToSnippets(List<Document> documents, AiKnowledgeDomain domain) {
        if (documents == null || documents.isEmpty()) {
            return List.of();
        }

        List<AiKnowledgeSnippet> snippets = new ArrayList<>();
        for (Document document : documents) {
            Map<String, Object> metadata = document.getMetadata();
            String knowledgeDomain = stringValue(metadata.get("knowledgeDomain"));
            if (!domain.name().equals(knowledgeDomain)) {
                continue;
            }

            snippets.add(new AiKnowledgeSnippet(
                    longValue(metadata.get("documentId")),
                    longValue(metadata.get("chunkId")),
                    stringValue(metadata.get("title")),
                    stringValue(metadata.get("sourceType")),
                    stringValue(metadata.get("sourceRef")),
                    truncateSnippet(document.getText()),
                    knowledgeDomain
            ));
            if (snippets.size() >= Math.max(1, aiProperties.getRag().getTopK())) {
                break;
            }
        }
        return snippets;
    }

    private int resolveCandidateCount() {
        int topK = Math.max(1, aiProperties.getRag().getTopK());
        return Math.max(topK * 3, topK);
    }

    private double resolveSimilarityThreshold() {
        Double minScore = aiProperties.getRag().getMinScore();
        if (minScore == null) {
            return 0D;
        }
        return Math.max(0D, Math.min(1D, minScore));
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private Long longValue(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String stringValue && StringUtils.hasText(stringValue)) {
            try {
                return Long.parseLong(stringValue.trim());
            } catch (NumberFormatException ignored) {
                return 0L;
            }
        }
        return 0L;
    }

    private String truncateSnippet(String content) {
        if (!StringUtils.hasText(content)) {
            return "";
        }
        String trimmed = content.trim();
        return trimmed.length() <= 160 ? trimmed : trimmed.substring(0, 160);
    }
}

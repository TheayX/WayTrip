package com.travel.service.ai.rag;

import com.travel.config.ai.AiProperties;
import com.travel.config.ai.AiVectorIndexHealth;
import com.travel.config.ai.AiVectorIndexHealthService;
import com.travel.enums.ai.AiScenarioType;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Redis 向量检索服务测试。
 */
class RedisVectorAiKnowledgeRetrievalServiceTest {

    @Test
    void retrieveShouldBoostAccountBoundaryForUnauthenticatedOrderQuestion() {
        VectorStore vectorStore = mock(VectorStore.class);
        AiVectorIndexHealthService healthService = mock(AiVectorIndexHealthService.class);
        AiProperties aiProperties = buildEnabledAiProperties();
        when(healthService.inspect()).thenReturn(retrievalReadyHealth());
        when(vectorStore.similaritySearch(any(SearchRequest.class)))
                .thenReturn(List.of(buildPolicyDocument(), buildToolPriorityDocument()))
                .thenReturn(List.of(buildAccountBoundaryDocument()));

        RedisVectorAiKnowledgeRetrievalService service =
                new RedisVectorAiKnowledgeRetrievalService(aiProperties, vectorStore, healthService);

        List<AiKnowledgeSnippet> snippets = service.retrieve(AiScenarioType.ORDER_ADVISOR, "没登录能帮我看订单吗");

        assertEquals(3, snippets.size());
        assertEquals("登录与个人数据边界", snippets.get(0).getTitle());
        assertEquals("ACCOUNT_HELP", snippets.get(0).getKnowledgeDomain());
        verify(vectorStore, times(2)).similaritySearch(any(SearchRequest.class));
    }

    @Test
    void retrieveShouldKeepDefaultSingleSearchForNormalOrderQuestion() {
        VectorStore vectorStore = mock(VectorStore.class);
        AiVectorIndexHealthService healthService = mock(AiVectorIndexHealthService.class);
        AiProperties aiProperties = buildEnabledAiProperties();
        when(healthService.inspect()).thenReturn(retrievalReadyHealth());
        when(vectorStore.similaritySearch(any(SearchRequest.class)))
                .thenReturn(List.of(buildPolicyDocument(), buildToolPriorityDocument()));

        RedisVectorAiKnowledgeRetrievalService service =
                new RedisVectorAiKnowledgeRetrievalService(aiProperties, vectorStore, healthService);

        List<AiKnowledgeSnippet> snippets = service.retrieve(AiScenarioType.ORDER_ADVISOR, "我昨天那笔订单能退吗");

        assertEquals(2, snippets.size());
        assertTrue(snippets.stream().allMatch(item -> "PLATFORM_POLICY".equals(item.getKnowledgeDomain())));
        verify(vectorStore, times(1)).similaritySearch(any(SearchRequest.class));
    }

    private AiProperties buildEnabledAiProperties() {
        AiProperties aiProperties = new AiProperties();
        aiProperties.getRag().setEnabled(Boolean.TRUE);
        aiProperties.getRag().setTopK(3);
        aiProperties.getRag().setMinScore(0D);
        return aiProperties;
    }

    private AiVectorIndexHealth retrievalReadyHealth() {
        return new AiVectorIndexHealth(true, true, 768, 768, true, true, false, "");
    }

    private Document buildPolicyDocument() {
        return new Document(
                "订单退款、取消、改签等售后问题，应优先查询真实订单数据再回答。",
                Map.of(
                        "documentId", 1L,
                        "chunkId", 8L,
                        "title", "订单售后边界",
                        "sourceType", "manual",
                        "sourceRef", "policy:order-boundary",
                        "knowledgeDomain", "PLATFORM_POLICY",
                        "knowledgeLayer", "boundary"
                )
        );
    }

    private Document buildToolPriorityDocument() {
        return new Document(
                "订单、景点、推荐、画像、运营等问题，只要系统工具能查到真实数据，就先使用工具结果回答。",
                Map.of(
                        "documentId", 4L,
                        "chunkId", 31L,
                        "title", "工具优先原则",
                        "sourceType", "manual",
                        "sourceRef", "strategy:tool-first",
                        "knowledgeDomain", "PLATFORM_POLICY",
                        "knowledgeLayer", "strategy"
                )
        );
    }

    private Document buildAccountBoundaryDocument() {
        return new Document(
                "涉及我的订单、收藏、账号、画像等个人数据时，用户必须先登录。未登录状态下只能回答通用帮助。",
                Map.of(
                        "documentId", 2L,
                        "chunkId", 18L,
                        "title", "登录与个人数据边界",
                        "sourceType", "manual",
                        "sourceRef", "account:login-boundary",
                        "knowledgeDomain", "ACCOUNT_HELP",
                        "knowledgeLayer", "boundary"
                )
        );
    }
}

package com.travel.service.ai.rag;

import com.travel.config.ai.AiProperties;
import com.travel.config.ai.AiVectorIndexHealth;
import com.travel.config.ai.AiVectorIndexHealthService;
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
import java.util.LinkedHashMap;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 基于 Redis Vector Store 的 AI 知识检索服务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisVectorAiKnowledgeRetrievalService implements AiKnowledgeRetrievalService {

    /**
     * AI 配置，控制 RAG 开关、召回数量和阈值。
     */
    private final AiProperties aiProperties;

    /**
     * 向量检索入口。
     */
    private final VectorStore vectorStore;

    /**
     * 向量索引健康检查服务。
     */
    private final AiVectorIndexHealthService aiVectorIndexHealthService;

    /**
     * 按场景执行向量检索，并在命中后过滤到对应知识域。
     *
     * @param scenario 场景类型
     * @param userMessage 用户消息
     * @return 命中的知识片段
     */
    @Override
    public List<AiKnowledgeSnippet> retrieve(AiScenarioType scenario, String userMessage) {
        if (!Boolean.TRUE.equals(aiProperties.getRag().getEnabled()) || !StringUtils.hasText(userMessage)) {
            return List.of();
        }

        long startedAt = System.currentTimeMillis();
        String normalizedMessage = userMessage.trim();
        List<AiKnowledgeDomain> candidateDomains = resolveCandidateDomains(scenario, normalizedMessage);
        AiVectorIndexHealth health = aiVectorIndexHealthService.inspect();
        if (!health.isRetrievalReady()) {
            log.warn(
                    "AI 向量检索已降级为空结果：场景={}, 知识域={}, reason={}",
                    scenario,
                    candidateDomains,
                    health.getWarningMessage()
            );
            return List.of();
        }

        try {
            List<Document> documents = new ArrayList<>(searchDocuments(normalizedMessage));
            if (shouldBoostAccountBoundary(scenario, normalizedMessage)) {
                // 未登录订单边界问题优先补召回“登录与个人数据边界”，避免只命中售后规则。
                documents.addAll(searchDocuments(buildLoginBoundaryBoostQuery(normalizedMessage)));
            }
            List<AiKnowledgeSnippet> snippets = mapToSnippets(documents, candidateDomains);
            log.info(
                    "AI RAG 检索完成：场景={}, 知识域={}, 用户问题长度={}, 命中数量={}, 命中标题={}, 总耗时Ms={}",
                    scenario,
                    candidateDomains,
                    normalizedMessage.length(),
                    snippets.size(),
                    snippets.stream().map(AiKnowledgeSnippet::getTitle).toList(),
                    System.currentTimeMillis() - startedAt
            );
            return snippets;
        } catch (Exception exception) {
            log.error(
                    "AI 向量检索失败：场景={}, 知识域={}, 用户问题长度={}, 总耗时Ms={}",
                    scenario,
                    candidateDomains,
                    normalizedMessage.length(),
                    System.currentTimeMillis() - startedAt,
                    exception
            );
            return List.of();
        }
    }

    /**
     * 为不同业务场景指定默认知识域，避免跨域召回污染结果。
     *
     * @param scenario 场景类型
     * @return 默认知识域
     */
    @Override
    public AiKnowledgeDomain resolveDomain(AiScenarioType scenario) {
        return switch (scenario) {
            case ORDER_ADVISOR -> AiKnowledgeDomain.PLATFORM_POLICY;
            case SPOT_QA, TRAVEL_PLANNER, RECOMMENDATION_EXPLAINER -> AiKnowledgeDomain.SPOT_KNOWLEDGE;
            case GUIDE_QA -> AiKnowledgeDomain.GUIDE_KNOWLEDGE;
            default -> AiKnowledgeDomain.PLATFORM_POLICY;
        };
    }

    /**
     * 将向量库命中文档转换为业务侧可用的知识片段，并按知识域过滤。
     *
     * @param documents 原始命中文档
     * @param candidateDomains 当前允许命中的知识域
     * @return 过滤后的知识片段
     */
    private List<AiKnowledgeSnippet> mapToSnippets(List<Document> documents, List<AiKnowledgeDomain> candidateDomains) {
        if (documents == null || documents.isEmpty()) {
            return List.of();
        }

        Map<Long, AiKnowledgeSnippet> uniqueCandidates = new LinkedHashMap<>();
        Set<String> allowedDomains = candidateDomains.stream().map(Enum::name).collect(java.util.stream.Collectors.toSet());
        for (Document document : documents) {
            Map<String, Object> metadata = document.getMetadata();
            String knowledgeDomain = stringValue(metadata.get("knowledgeDomain"));
            if (!allowedDomains.contains(knowledgeDomain)) {
                continue;
            }

            AiKnowledgeSnippet snippet = new AiKnowledgeSnippet(
                    longValue(metadata.get("documentId")),
                    longValue(metadata.get("chunkId")),
                    stringValue(metadata.get("title")),
                    stringValue(metadata.get("sourceType")),
                    stringValue(metadata.get("sourceRef")),
                    truncateSnippet(document.getText()),
                    knowledgeDomain,
                    stringValue(metadata.get("knowledgeLayer"))
            );
            uniqueCandidates.putIfAbsent(snippet.getChunkId(), snippet);
        }
        List<AiKnowledgeSnippet> candidates = new ArrayList<>(uniqueCandidates.values());
        candidates.sort(buildSnippetComparator(candidateDomains));
        int topK = Math.max(1, aiProperties.getRag().getTopK());
        return candidates.size() <= topK ? candidates : new ArrayList<>(candidates.subList(0, topK));
    }

    /**
     * 解析当前检索应该允许命中的知识域。
     *
     * @param scenario 场景
     * @param userMessage 用户问题
     * @return 候选知识域
     */
    private List<AiKnowledgeDomain> resolveCandidateDomains(AiScenarioType scenario, String userMessage) {
        AiKnowledgeDomain defaultDomain = resolveDomain(scenario);
        if (shouldBoostAccountBoundary(scenario, userMessage)) {
            return List.of(AiKnowledgeDomain.ACCOUNT_HELP, defaultDomain);
        }
        return List.of(defaultDomain);
    }

    /**
     * 判断当前是否属于“未登录订单边界”问题。
     *
     * @param scenario 场景
     * @param userMessage 用户问题
     * @return 是否需要优先补召回账号边界
     */
    private boolean shouldBoostAccountBoundary(AiScenarioType scenario, String userMessage) {
        if (!supportsAccountBoundaryBoost(scenario) || !StringUtils.hasText(userMessage)) {
            return false;
        }
        String normalized = userMessage.trim();
        return containsAny(normalized, "没登录", "未登录", "不登录", "先登录", "登录后")
                && containsAny(normalized, "订单", "看订单", "查订单", "订单详情", "订单记录");
    }

    /**
     * 判断当前场景是否允许启用“登录与个人数据边界”补召回。
     *
     * @param scenario 场景
     * @return 是否允许补召回
     */
    private boolean supportsAccountBoundaryBoost(AiScenarioType scenario) {
        return scenario == AiScenarioType.ORDER_ADVISOR || scenario == AiScenarioType.CUSTOMER_SERVICE;
    }

    /**
     * 构造登录边界补召回查询，主动把“登录、个人订单、隐私”这些边界语义补进向量搜索。
     *
     * @param userMessage 原始问题
     * @return 扩展后的查询
     */
    private String buildLoginBoundaryBoostQuery(String userMessage) {
        return userMessage + " 未登录 登录 个人订单 个人数据 隐私 账号 边界";
    }

    /**
     * 执行一次向量检索请求。
     *
     * @param query 检索问题
     * @return 原始命中文档
     */
    private List<Document> searchDocuments(String query) {
        SearchRequest request = SearchRequest.builder()
                .query(query)
                .topK(resolveCandidateCount())
                .similarityThreshold(resolveSimilarityThreshold())
                .build();
        List<Document> documents = vectorStore.similaritySearch(request);
        return documents == null ? List.of() : documents;
    }

    /**
     * 构造知识片段排序器；当存在账号边界补召回时，优先把账号边界类知识排到前面。
     *
     * @param candidateDomains 候选知识域
     * @return 片段排序器
     */
    private Comparator<AiKnowledgeSnippet> buildSnippetComparator(List<AiKnowledgeDomain> candidateDomains) {
        Map<String, Integer> domainPriority = new LinkedHashMap<>();
        for (int i = 0; i < candidateDomains.size(); i++) {
            domainPriority.put(candidateDomains.get(i).name(), i);
        }
        return Comparator
                .comparingInt((AiKnowledgeSnippet item) -> domainPriority.getOrDefault(item.getKnowledgeDomain(), Integer.MAX_VALUE))
                .thenComparingInt(item -> AiKnowledgeLayerSupport.priority(item.getKnowledgeLayer()))
                .thenComparing(AiKnowledgeSnippet::getTitle, String.CASE_INSENSITIVE_ORDER);
    }

    /**
     * 为向量召回预留一定候选空间，再由业务侧按知识域和 topK 二次收敛。
     *
     * @return 候选召回数量
     */
    private int resolveCandidateCount() {
        int topK = Math.max(1, aiProperties.getRag().getTopK());
        return Math.max(topK * 3, topK);
    }

    /**
     * 解析相似度阈值，并强制收敛到 0 到 1 之间。
     *
     * @return 相似度阈值
     */
    private double resolveSimilarityThreshold() {
        Double minScore = aiProperties.getRag().getMinScore();
        if (minScore == null) {
            return 0D;
        }
        return Math.max(0D, Math.min(1D, minScore));
    }

    /**
     * 将元数据安全转换为字符串。
     *
     * @param value 元数据值
     * @return 字符串结果
     */
    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    /**
     * 将元数据安全转换为 Long。
     *
     * @param value 元数据值
     * @return Long 结果；无法解析时返回 0
     */
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

    /**
     * 裁剪片段文本，避免过长知识块挤占系统提示词空间。
     *
     * @param content 原始文本
     * @return 裁剪后的片段
     */
    private String truncateSnippet(String content) {
        if (!StringUtils.hasText(content)) {
            return "";
        }
        String trimmed = content.trim();
        return trimmed.length() <= 160 ? trimmed : trimmed.substring(0, 160);
    }

    /**
     * 判断文本是否包含任一关键词。
     *
     * @param source 原始文本
     * @param keywords 关键词列表
     * @return 是否命中
     */
    private boolean containsAny(String source, String... keywords) {
        if (!StringUtils.hasText(source)) {
            return false;
        }
        for (String keyword : keywords) {
            if (source.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}

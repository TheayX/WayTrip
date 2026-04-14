package com.travel.config.ai;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * AI 模块统一配置，收口模型、会话、检索和风控参数。
 */
@Data
@ConfigurationProperties(prefix = "app.ai")
public class AiProperties {

    private ProviderProperties provider = new ProviderProperties();
    private ChatProperties chat = new ChatProperties();
    private ToolProperties tool = new ToolProperties();
    private RagProperties rag = new RagProperties();
    private VectorProperties vector = new VectorProperties();
    private MemoryProperties memory = new MemoryProperties();
    private GuardrailProperties guardrail = new GuardrailProperties();
    private MetricsProperties metrics = new MetricsProperties();

    @Data
    public static class ProviderProperties {
        private String type = "ollama";
        private String chatModel = "qwen2.5:1.5b";
        private String embeddingModel = "nomic-embed-text";
    }

    @Data
    public static class ChatProperties {
        private Integer timeoutSeconds = 60;
        private Integer maxInputLength = 800;
        private Integer maxHistoryRounds = 8;
        private String systemPrompt = "你是 WayTrip 旅游助手。请始终使用简体中文，回答简洁、友好、可执行。"
                + "涉及价格、库存、退款、时效时，必须明确说明以页面规则和系统真实数据为准。";
    }

    @Data
    public static class ToolProperties {
        private Boolean enabled = Boolean.TRUE;
        private Integer maxCallsPerRequest = 6;
    }

    @Data
    public static class RagProperties {
        private Boolean enabled = Boolean.FALSE;
        private Integer topK = 4;
        private Double minScore = 0.65D;
    }

    @Data
    public static class VectorProperties {
        private RedisProperties redis = new RedisProperties();
    }

    @Data
    public static class RedisProperties {
        private String host = "127.0.0.1";
        private Integer port = 6379;
        private String password = "";
        private String clientName = "waytrip-ai-vector";
        private Integer timeoutSeconds = 3;
        private Boolean sslEnabled = Boolean.FALSE;
        private String indexName = "waytrip-ai-knowledge-index";
        private String prefix = "waytrip:ai:chunk:";
        private Boolean initializeSchema = Boolean.TRUE;
    }

    @Data
    public static class MemoryProperties {
        private Integer ttlMinutes = 60;
        private Integer historyRounds = 8;
        private Boolean summaryEnabled = Boolean.TRUE;
    }

    @Data
    public static class GuardrailProperties {
        private Integer rateLimitIpPerMinute = 24;
        private Integer rateLimitSessionPerMinute = 12;
    }

    @Data
    public static class MetricsProperties {
        private Boolean enabled = Boolean.TRUE;
    }
}

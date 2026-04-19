package com.travel.config.ai;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * AI 模块统一配置，收口模型、会话、检索和风控参数。
 */
@Data
@ConfigurationProperties(prefix = "app.ai")
public class AiProperties {

    /**
     * 最终答案生成模型配置。
     */
    private GenerationProperties generation = new GenerationProperties();

    /**
     * 意图识别模型配置。
     */
    private IntentProperties intent = new IntentProperties();

    /**
     * 嵌入模型配置。
     */
    private EmbeddingProperties embedding = new EmbeddingProperties();

    /**
     * 聊天交互配置，控制输入长度、超时和系统提示词。
     */
    private ChatProperties chat = new ChatProperties();

    /**
     * Tool Calling 配置，限制工具是否开启及单次最大调用次数。
     */
    private ToolProperties tool = new ToolProperties();

    /**
     * RAG 检索配置，控制知识库检索的开关与召回阈值。
     */
    private RagProperties rag = new RagProperties();

    /**
     * 向量索引配置，定义 Redis 向量库的连接与索引参数。
     */
    private VectorProperties vector = new VectorProperties();

    /**
     * 会话记忆配置，控制上下文保存时长与保留轮次。
     */
    private MemoryProperties memory = new MemoryProperties();

    /**
     * 风控配置，限制 AI 接口在 IP 和会话维度上的访问频率。
     */
    private GuardrailProperties guardrail = new GuardrailProperties();

    /**
     * 指标采集配置，控制 AI 模块是否开启埋点统计。
     */
    private MetricsProperties metrics = new MetricsProperties();

    /**
     * 模型提供方配置。
     */
    @Data
    public static class GenerationProperties {

        /**
         * 生成模型提供方类型。
         */
        private String provider = "openai";

        /**
         * 生成模型名称。
         */
        private String model = "qwen3.5-plus";

        /**
         * 生成模型基础地址。
         */
        private String baseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1";

        /**
         * 生成模型 API Key。
         */
        private String apiKey = "";

        /**
         * 生成模型聊天完成路径。
         */
        private String completionsPath = "/chat/completions";

        /**
         * 生成模型温度。
         */
        private Double temperature = 0.2D;
    }

    /**
     * 意图识别模型配置。
     */
    @Data
    public static class IntentProperties {

        /**
         * 意图模型提供方类型。
         */
        private String provider = "ollama";

        /**
         * 意图模型名称。
         */
        private String model = "qwen2.5:1.5b";

        /**
         * 意图模型基础地址。
         */
        private String baseUrl = "http://127.0.0.1:11434";

        /**
         * 意图模型温度。
         */
        private Double temperature = 0D;
    }

    /**
     * 嵌入模型配置。
     */
    @Data
    public static class EmbeddingProperties {

        /**
         * 嵌入模型提供方类型。
         */
        private String provider = "ollama";

        /**
         * 嵌入模型名称。
         */
        private String model = "nomic-embed-text";

        /**
         * 嵌入模型基础地址。
         */
        private String baseUrl = "http://127.0.0.1:11434";

        /**
         * 嵌入模型接口路径。
         */
        private String embeddingsPath = "/v1/embeddings";

        /**
         * 嵌入模型 API Key。
         */
        private String apiKey = "";
    }

    /**
     * 聊天行为配置。
     */
    @Data
    public static class ChatProperties {

        /**
         * 单次模型调用超时时间，单位：秒。
         */
        private Integer timeoutSeconds = 60;

        /**
         * 单次用户输入允许的最大字符长度。
         */
        private Integer maxInputLength = 800;

        /**
         * 组装提示词时最多携带的历史对话轮数。
         */
        private Integer maxHistoryRounds = 8;

        /**
         * 系统提示词，统一约束助手语气、语言和业务边界。
         */
        private String systemPrompt = "你是 WayTrip 旅游助手。请始终使用简体中文，回答简洁、友好、可执行。"
                + "涉及价格、库存、退款、时效时，必须明确说明以页面规则和系统真实数据为准。";
    }

    /**
     * Tool Calling 配置。
     */
    @Data
    public static class ToolProperties {

        /**
         * 是否启用工具调用能力。
         */
        private Boolean enabled = Boolean.TRUE;

        /**
         * 单次请求最多允许触发的工具调用次数。
         */
        private Integer maxCallsPerRequest = 6;
    }

    /**
     * 检索增强生成配置。
     */
    @Data
    public static class RagProperties {

        /**
         * 是否启用知识库检索增强。
         */
        private Boolean enabled = Boolean.FALSE;

        /**
         * 单次检索最多返回的知识片段数量。
         */
        private Integer topK = 4;

        /**
         * 检索结果的最低相似度阈值。
         */
        private Double minScore = 0.65D;
    }

    /**
     * 向量索引配置。
     */
    @Data
    public static class VectorProperties {

        /**
         * Redis 向量库连接配置。
         */
        private RedisProperties redis = new RedisProperties();
    }

    /**
     * Redis 向量索引连接配置。
     */
    @Data
    public static class RedisProperties {

        /**
         * Redis 主机地址。
         */
        private String host = "127.0.0.1";

        /**
         * Redis 端口。
         */
        private Integer port = 6379;

        /**
         * Redis 访问密码，留空表示不鉴权。
         */
        private String password = "";

        /**
         * Redis 客户端名称，便于服务端识别来源。
         */
        private String clientName = "waytrip-ai-vector";

        /**
         * Redis 连接超时时间，单位：秒。
         */
        private Integer timeoutSeconds = 3;

        /**
         * 是否启用 SSL 连接 Redis。
         */
        private Boolean sslEnabled = Boolean.FALSE;

        /**
         * RedisSearch 向量索引名称。
         */
        private String indexName = "waytrip-ai-knowledge-index";

        /**
         * 向量文档主键前缀。
         */
        private String prefix = "waytrip:ai:chunk:";

        /**
         * 初始化向量存储时是否自动建索引。
         */
        private Boolean initializeSchema = Boolean.TRUE;

        /**
         * Redis 向量索引算法，可选值：HNSW、FLAT。
         */
        private String algorithm = "HNSW";
    }

    /**
     * 会话记忆配置。
     */
    @Data
    public static class MemoryProperties {

        /**
         * 会话上下文在 Redis 中的保留时长，单位：分钟。
         */
        private Integer ttlMinutes = 60;

        /**
         * 会话记忆中最多保留的历史轮数。
         */
        private Integer historyRounds = 8;

        /**
         * 是否启用记忆摘要能力。
         */
        private Boolean summaryEnabled = Boolean.TRUE;
    }

    /**
     * AI 风控配置。
     */
    @Data
    public static class GuardrailProperties {

        /**
         * 单个 IP 每分钟允许的最大请求次数。
         */
        private Integer rateLimitIpPerMinute = 24;

        /**
         * 单个会话每分钟允许的最大请求次数。
         */
        private Integer rateLimitSessionPerMinute = 12;
    }

    /**
     * AI 指标配置。
     */
    @Data
    public static class MetricsProperties {

        /**
         * 是否启用 AI 指标采集。
         */
        private Boolean enabled = Boolean.TRUE;
    }
}

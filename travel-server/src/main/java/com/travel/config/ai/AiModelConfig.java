package com.travel.config.ai;

import io.micrometer.observation.ObservationRegistry;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.BatchingStrategy;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.TokenCountBatchingStrategy;
import org.springframework.ai.model.SimpleApiKey;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.observation.VectorStoreObservationConvention;
import org.springframework.ai.vectorstore.redis.RedisVectorStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.JedisPooled;

import java.time.Duration;
import java.util.List;

/**
 * AI 模型基础配置，显式拆分生成、意图和嵌入三段模型。
 */
@Configuration
@EnableConfigurationProperties(AiProperties.class)
public class AiModelConfig {

    /**
     * AI 流式响应执行器。
     * <p>
     * 流式输出会长时间占用连接，因此单独分配线程池，避免与普通 Web 请求争抢线程。
     *
     * @return 线程池执行器
     */
    @Bean("aiChatStreamExecutor")
    public TaskExecutor aiChatStreamExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("ai-chat-stream-");
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(32);
        executor.initialize();
        return executor;
    }

    /**
     * AI 双轨并行执行器。
     * <p>
     * 左轨检索和右轨上下文预处理都属于请求内短任务，不应与流式输出线程长期混用。
     *
     * @return 线程池执行器
     */
    @Bean("aiChatPipelineExecutor")
    public TaskExecutor aiChatPipelineExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("ai-chat-pipeline-");
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(64);
        executor.initialize();
        return executor;
    }

    /**
     * AI 知识任务执行器。
     *
     * @return 线程池执行器
     */
    @Bean("aiKnowledgeJobExecutor")
    public TaskExecutor aiKnowledgeJobExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("ai-knowledge-job-");
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(64);
        executor.initialize();
        return executor;
    }

    /**
     * AI 知识 Stream 监听执行器。
     * <p>
     * Redis Stream 轮询应与实际任务处理线程分离，避免关闭阶段和任务堆积相互影响。
     *
     * @return 线程池执行器
     */
    @Bean("aiKnowledgeStreamExecutor")
    public TaskExecutor aiKnowledgeStreamExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("ai-knowledge-stream-");
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(8);
        executor.initialize();
        return executor;
    }

    /**
     * 注册最终答案生成模型。
     *
     * @param aiProperties AI 配置
     * @param observationRegistry 观测注册表
     * @param restClientBuilder 同步 HTTP 客户端构建器
     * @param webClientBuilder 响应式 HTTP 客户端构建器
     * @param responseErrorHandler Spring AI 默认错误处理器
     * @return 聊天模型
     */
    @Bean("generationChatModel")
    public ChatModel generationChatModel(AiProperties aiProperties,
                                         ObjectProvider<ObservationRegistry> observationRegistry,
                                         RestClient.Builder restClientBuilder,
                                         WebClient.Builder webClientBuilder,
                                         ObjectProvider<ResponseErrorHandler> responseErrorHandler) {
        return buildChatModel(
                aiProperties.getGeneration().getProvider(),
                aiProperties.getGeneration().getBaseUrl(),
                aiProperties.getGeneration().getApiKey(),
                aiProperties.getGeneration().getCompletionsPath(),
                aiProperties.getEmbedding().getEmbeddingsPath(),
                aiProperties.getGeneration().getModel(),
                aiProperties.getGeneration().getTemperature(),
                observationRegistry.getIfUnique(() -> ObservationRegistry.NOOP),
                restClientBuilder,
                webClientBuilder,
                responseErrorHandler.getIfAvailable()
        );
    }

    /**
     * 注册意图识别模型。
     *
     * @param aiProperties AI 配置
     * @param observationRegistry 观测注册表
     * @param restClientBuilder 同步 HTTP 客户端构建器
     * @param webClientBuilder 响应式 HTTP 客户端构建器
     * @param responseErrorHandler Spring AI 默认错误处理器
     * @return 聊天模型
     */
    @Bean("intentChatModel")
    public ChatModel intentChatModel(AiProperties aiProperties,
                                     ObjectProvider<ObservationRegistry> observationRegistry,
                                     RestClient.Builder restClientBuilder,
                                     WebClient.Builder webClientBuilder,
                                     ObjectProvider<ResponseErrorHandler> responseErrorHandler) {
        return buildChatModel(
                aiProperties.getIntent().getProvider(),
                aiProperties.getIntent().getBaseUrl(),
                "",
                "/v1/chat/completions",
                aiProperties.getEmbedding().getEmbeddingsPath(),
                aiProperties.getIntent().getModel(),
                aiProperties.getIntent().getTemperature(),
                observationRegistry.getIfUnique(() -> ObservationRegistry.NOOP),
                restClientBuilder,
                webClientBuilder,
                responseErrorHandler.getIfAvailable()
        );
    }

    /**
     * 注册最终生成链路使用的 ChatClient。
     *
     * @param generationChatModel 最终生成模型
     * @param chatMemory 对话记忆实现
     * @param observationRegistry 观测注册表
     * @return ChatClient 实例
     */
    @Bean("aiChatClient")
    public ChatClient aiChatClient(@Qualifier("generationChatModel") ChatModel generationChatModel,
                                   ChatMemory chatMemory,
                                   ObjectProvider<ObservationRegistry> observationRegistry) {
        return ChatClient.builder(generationChatModel, observationRegistry.getIfUnique(() -> ObservationRegistry.NOOP), null)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
    }

    /**
     * 注册意图识别专用 ChatClient。
     *
     * @param intentChatModel 意图识别模型
     * @param observationRegistry 观测注册表
     * @return ChatClient 实例
     */
    @Bean("intentChatClient")
    public ChatClient intentChatClient(@Qualifier("intentChatModel") ChatModel intentChatModel,
                                       ObjectProvider<ObservationRegistry> observationRegistry) {
        return ChatClient.builder(intentChatModel, observationRegistry.getIfUnique(() -> ObservationRegistry.NOOP), null)
                .build();
    }

    /**
     * 注册 AI 嵌入批处理策略。
     *
     * @return 批处理策略
     */
    @Bean
    public BatchingStrategy aiEmbeddingBatchingStrategy() {
        return new TokenCountBatchingStrategy();
    }

    /**
     * 注册嵌入模型，供向量入库和查询向量化共用。
     *
     * @param aiProperties AI 配置
     * @param observationRegistry 观测注册表
     * @param restClientBuilder 同步 HTTP 客户端构建器
     * @param webClientBuilder 响应式 HTTP 客户端构建器
     * @param responseErrorHandler Spring AI 默认错误处理器
     * @return 嵌入模型
     */
    @Bean
    @Primary
    public EmbeddingModel embeddingModel(AiProperties aiProperties,
                                         ObjectProvider<ObservationRegistry> observationRegistry,
                                         RestClient.Builder restClientBuilder,
                                         WebClient.Builder webClientBuilder,
                                         ObjectProvider<ResponseErrorHandler> responseErrorHandler) {
        return buildEmbeddingModel(
                aiProperties.getEmbedding().getProvider(),
                aiProperties.getEmbedding().getBaseUrl(),
                aiProperties.getEmbedding().getApiKey(),
                aiProperties.getEmbedding().getEmbeddingsPath(),
                aiProperties.getEmbedding().getModel(),
                observationRegistry.getIfUnique(() -> ObservationRegistry.NOOP),
                restClientBuilder,
                webClientBuilder,
                responseErrorHandler.getIfAvailable()
        );
    }

    /**
     * 注册 AI 向量检索专用 Redis 连接。
     *
     * @param aiProperties AI 配置
     * @return Jedis 连接
     */
    @Bean(destroyMethod = "close")
    public JedisPooled aiVectorJedisPooled(AiProperties aiProperties) {
        return createJedisPooled(aiProperties.getVector().getRedis());
    }

    /**
     * 注册 AI 专用向量存储，统一承载知识库分片索引。
     *
     * @param embeddingModel 嵌入模型
     * @param aiVectorJedisPooled AI 向量 Redis 连接
     * @param aiProperties AI 配置
     * @param observationRegistry 观测注册表
     * @param customObservationConvention 自定义观测约定
     * @param batchingStrategy 批处理策略
     * @return 向量存储
     */
    @Bean
    @Primary
    public VectorStore aiVectorStore(EmbeddingModel embeddingModel,
                                     JedisPooled aiVectorJedisPooled,
                                     AiProperties aiProperties,
                                     ObjectProvider<ObservationRegistry> observationRegistry,
                                     ObjectProvider<VectorStoreObservationConvention> customObservationConvention,
                                     BatchingStrategy batchingStrategy) {
        AiProperties.RedisProperties redisProperties = aiProperties.getVector().getRedis();
        return RedisVectorStore.builder(aiVectorJedisPooled, embeddingModel)
                .initializeSchema(Boolean.TRUE.equals(redisProperties.getInitializeSchema()))
                .observationRegistry(observationRegistry.getIfUnique(() -> ObservationRegistry.NOOP))
                .customObservationConvention(customObservationConvention.getIfAvailable(() -> null))
                .batchingStrategy(batchingStrategy)
                .indexName(redisProperties.getIndexName())
                .prefix(redisProperties.getPrefix())
                .vectorAlgorithm(resolveVectorAlgorithm(redisProperties.getAlgorithm()))
                // 元数据字段需要和知识分片写入时的 metadata key 保持一致，否则过滤和回溯会失效。
                .metadataFields(List.of(
                        RedisVectorStore.MetadataField.numeric("documentId"),
                        RedisVectorStore.MetadataField.numeric("chunkId"),
                        RedisVectorStore.MetadataField.tag("knowledgeDomain"),
                        RedisVectorStore.MetadataField.tag("knowledgeLayer"),
                        RedisVectorStore.MetadataField.text("title"),
                        RedisVectorStore.MetadataField.tag("sourceType"),
                        RedisVectorStore.MetadataField.text("sourceRef"),
                        RedisVectorStore.MetadataField.text("tags")
                ))
                .build();
    }

    /**
     * 根据配置构建聊天模型，避免意图识别和最终生成复用同一客户端。
     *
     * @param provider 提供方类型
     * @param baseUrl 服务地址
     * @param apiKey API Key
     * @param completionsPath 聊天完成路径
     * @param embeddingsPath 向量路径
     * @param model 模型名称
     * @param temperature 温度
     * @param observationRegistry 观测注册表
     * @param restClientBuilder 同步 HTTP 客户端构建器
     * @param webClientBuilder 响应式 HTTP 客户端构建器
     * @param responseErrorHandler 错误处理器
     * @return 聊天模型
     */
    private ChatModel buildChatModel(String provider,
                                     String baseUrl,
                                     String apiKey,
                                     String completionsPath,
                                     String embeddingsPath,
                                     String model,
                                     Double temperature,
                                     ObservationRegistry observationRegistry,
                                     RestClient.Builder restClientBuilder,
                                     WebClient.Builder webClientBuilder,
                                     ResponseErrorHandler responseErrorHandler) {
        if (!StringUtils.hasText(provider)) {
            throw new IllegalArgumentException("AI 模型提供方不能为空");
        }
        if ("ollama".equalsIgnoreCase(provider)) {
            return OllamaChatModel.builder()
                    .ollamaApi(buildOllamaApi(baseUrl, restClientBuilder, webClientBuilder, responseErrorHandler))
                    .defaultOptions(OllamaOptions.builder().model(model).temperature(temperature).build())
                    .observationRegistry(observationRegistry)
                    .build();
        }
        if ("openai".equalsIgnoreCase(provider)) {
            return OpenAiChatModel.builder()
                    .openAiApi(buildOpenAiApi(baseUrl, apiKey, completionsPath, embeddingsPath, restClientBuilder, webClientBuilder, responseErrorHandler))
                    .defaultOptions(OpenAiChatOptions.builder().model(model).temperature(temperature).build())
                    .observationRegistry(observationRegistry)
                    .build();
        }
        throw new IllegalArgumentException("暂不支持的 AI 聊天模型提供方: " + provider);
    }

    /**
     * 根据配置构建嵌入模型，方便后续独立替换 embedding 供应商。
     *
     * @param provider 提供方类型
     * @param baseUrl 服务地址
     * @param apiKey API Key
     * @param embeddingsPath 向量路径
     * @param model 模型名称
     * @param observationRegistry 观测注册表
     * @param restClientBuilder 同步 HTTP 客户端构建器
     * @param webClientBuilder 响应式 HTTP 客户端构建器
     * @param responseErrorHandler 错误处理器
     * @return 嵌入模型
     */
    private EmbeddingModel buildEmbeddingModel(String provider,
                                               String baseUrl,
                                               String apiKey,
                                               String embeddingsPath,
                                               String model,
                                               ObservationRegistry observationRegistry,
                                               RestClient.Builder restClientBuilder,
                                               WebClient.Builder webClientBuilder,
                                               ResponseErrorHandler responseErrorHandler) {
        if (!StringUtils.hasText(provider)) {
            throw new IllegalArgumentException("AI 嵌入模型提供方不能为空");
        }
        if ("ollama".equalsIgnoreCase(provider)) {
            return OllamaEmbeddingModel.builder()
                    .ollamaApi(buildOllamaApi(baseUrl, restClientBuilder, webClientBuilder, responseErrorHandler))
                    .defaultOptions(OllamaOptions.builder().model(model).build())
                    .observationRegistry(observationRegistry)
                    .build();
        }
        if ("openai".equalsIgnoreCase(provider)) {
            return new OpenAiEmbeddingModel(
                    buildOpenAiApi(baseUrl, apiKey, "/v1/chat/completions", embeddingsPath, restClientBuilder, webClientBuilder, responseErrorHandler),
                    org.springframework.ai.document.MetadataMode.EMBED,
                    OpenAiEmbeddingOptions.builder().model(model).build(),
                    org.springframework.ai.retry.RetryUtils.DEFAULT_RETRY_TEMPLATE,
                    observationRegistry
            );
        }
        throw new IllegalArgumentException("暂不支持的 AI 嵌入模型提供方: " + provider);
    }

    /**
     * 构建 OpenAI 风格 API 客户端。
     *
     * @param baseUrl 服务地址
     * @param apiKey API Key
     * @param completionsPath 聊天完成路径
     * @param embeddingsPath 向量路径
     * @param restClientBuilder 同步 HTTP 客户端构建器
     * @param webClientBuilder 响应式 HTTP 客户端构建器
     * @param responseErrorHandler 错误处理器
     * @return OpenAI API 客户端
     */
    private OpenAiApi buildOpenAiApi(String baseUrl,
                                     String apiKey,
                                     String completionsPath,
                                     String embeddingsPath,
                                     RestClient.Builder restClientBuilder,
                                     WebClient.Builder webClientBuilder,
                                     ResponseErrorHandler responseErrorHandler) {
        OpenAiApi.Builder builder = OpenAiApi.builder()
                .baseUrl(baseUrl)
                .apiKey(new SimpleApiKey(apiKey))
                .completionsPath(completionsPath)
                .embeddingsPath(embeddingsPath)
                .restClientBuilder(restClientBuilder)
                .webClientBuilder(webClientBuilder);
        if (responseErrorHandler != null) {
            builder.responseErrorHandler(responseErrorHandler);
        }
        return builder.build();
    }

    /**
     * 构建 Ollama API 客户端。
     *
     * @param baseUrl 服务地址
     * @param restClientBuilder 同步 HTTP 客户端构建器
     * @param webClientBuilder 响应式 HTTP 客户端构建器
     * @param responseErrorHandler 错误处理器
     * @return Ollama API 客户端
     */
    private OllamaApi buildOllamaApi(String baseUrl,
                                     RestClient.Builder restClientBuilder,
                                     WebClient.Builder webClientBuilder,
                                     ResponseErrorHandler responseErrorHandler) {
        OllamaApi.Builder builder = OllamaApi.builder()
                .baseUrl(baseUrl)
                .restClientBuilder(restClientBuilder)
                .webClientBuilder(webClientBuilder);
        if (responseErrorHandler != null) {
            builder.responseErrorHandler(responseErrorHandler);
        }
        return builder.build();
    }

    /**
     * 解析 Redis 向量索引算法，默认使用主流的 HNSW。
     *
     * @param algorithm 配置值
     * @return RedisVectorStore 算法枚举
     */
    private RedisVectorStore.Algorithm resolveVectorAlgorithm(String algorithm) {
        if (!StringUtils.hasText(algorithm)) {
            return RedisVectorStore.Algorithm.HSNW;
        }
        return "FLAT".equalsIgnoreCase(algorithm.trim())
                ? RedisVectorStore.Algorithm.FLAT
                : RedisVectorStore.Algorithm.HSNW;
    }

    /**
     * 构建 AI 向量 Redis 专用连接。
     * <p>
     * 这里单独使用 JedisPooled，而不是复用 Spring Data Redis 连接，
     * 是因为 RedisVectorStore 直接依赖 Jedis 客户端执行 RedisSearch 命令。
     *
     * @param redisProperties Redis 向量配置
     * @return Jedis 连接池
     */
    private JedisPooled createJedisPooled(AiProperties.RedisProperties redisProperties) {
        int timeoutSeconds = redisProperties.getTimeoutSeconds() != null ? redisProperties.getTimeoutSeconds() : 3;
        Duration timeout = Duration.ofSeconds(Math.max(1, timeoutSeconds));
        DefaultJedisClientConfig.Builder builder = DefaultJedisClientConfig.builder()
                .ssl(Boolean.TRUE.equals(redisProperties.getSslEnabled()))
                .clientName(redisProperties.getClientName())
                .timeoutMillis((int) timeout.toMillis());
        if (StringUtils.hasText(redisProperties.getPassword())) {
            builder.password(redisProperties.getPassword());
        }
        JedisClientConfig clientConfig = builder.build();
        return new JedisPooled(
                new HostAndPort(redisProperties.getHost(), redisProperties.getPort()),
                clientConfig
        );
    }
}

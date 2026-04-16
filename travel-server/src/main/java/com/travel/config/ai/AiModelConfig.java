package com.travel.config.ai;

import io.micrometer.observation.ObservationRegistry;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.embedding.BatchingStrategy;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.TokenCountBatchingStrategy;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.observation.VectorStoreObservationConvention;
import org.springframework.ai.vectorstore.redis.RedisVectorStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.util.StringUtils;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.JedisPooled;

import java.time.Duration;
import java.util.List;

/**
 * AI 模型基础配置，统一暴露聊天客户端。
 */
@Configuration
@EnableConfigurationProperties(AiProperties.class)
public class AiModelConfig {

    /**
     * 注册统一 ChatClient，默认挂载消息记忆 advisor，后续 Tool Calling 和 RAG 也围绕该客户端扩展。
     *
     * @param builder Spring AI 自动配置的 ChatClient 构建器
     * @param chatMemory 对话记忆实现
     * @return ChatClient 实例
     */
    @Bean
    public ChatClient aiChatClient(ChatClient.Builder builder, ChatMemory chatMemory) {
        return builder
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
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
     * 注册 AI 专用向量存储，统一承载知识库分片索引。
     *
     * @param embeddingModel 嵌入模型
     * @param aiProperties AI 配置
     * @param observationRegistry 观测注册表
     * @param customObservationConvention 自定义观测约定
     * @param batchingStrategy 批处理策略
     * @return 向量存储
     */
    @Bean
    @Primary
    public VectorStore aiVectorStore(EmbeddingModel embeddingModel,
                                     AiProperties aiProperties,
                                     ObjectProvider<ObservationRegistry> observationRegistry,
                                     ObjectProvider<VectorStoreObservationConvention> customObservationConvention,
                                     BatchingStrategy batchingStrategy) {
        AiProperties.RedisProperties redisProperties = aiProperties.getVector().getRedis();
        return RedisVectorStore.builder(createJedisPooled(redisProperties), embeddingModel)
                .initializeSchema(Boolean.TRUE.equals(redisProperties.getInitializeSchema()))
                .observationRegistry(observationRegistry.getIfUnique(() -> ObservationRegistry.NOOP))
                .customObservationConvention(customObservationConvention.getIfAvailable(() -> null))
                .batchingStrategy(batchingStrategy)
                .indexName(redisProperties.getIndexName())
                .prefix(redisProperties.getPrefix())
                .metadataFields(List.of(
                        RedisVectorStore.MetadataField.numeric("documentId"),
                        RedisVectorStore.MetadataField.numeric("chunkId"),
                        RedisVectorStore.MetadataField.tag("knowledgeDomain"),
                        RedisVectorStore.MetadataField.text("title"),
                        RedisVectorStore.MetadataField.tag("sourceType"),
                        RedisVectorStore.MetadataField.text("sourceRef")
                ))
                .build();
    }

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

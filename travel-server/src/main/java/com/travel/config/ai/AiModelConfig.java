package com.travel.config.ai;

import io.micrometer.observation.ObservationRegistry;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.embedding.BatchingStrategy;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.TokenCountBatchingStrategy;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.observation.VectorStoreObservationConvention;
import org.springframework.ai.vectorstore.redis.RedisVectorStore;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.ai.vectorstore.redis.autoconfigure.RedisVectorStoreProperties;
import org.springframework.beans.factory.ObjectProvider;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.JedisPooled;

import java.util.List;

/**
 * AI 模型基础配置，统一暴露聊天客户端。
 */
@Configuration
@EnableConfigurationProperties(AiProperties.class)
public class AiModelConfig {

    /**
     * 注册统一 ChatClient，后续 Tool Calling 和 RAG 也围绕该客户端扩展。
     *
     * @param builder Spring AI 自动配置的 ChatClient 构建器
     * @return ChatClient 实例
     */
    @Bean
    public ChatClient aiChatClient(ChatClient.Builder builder) {
        return builder.build();
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
     * @param jedisConnectionFactory Redis 连接工厂
     * @param properties Redis 向量存储配置
     * @param observationRegistry 观测注册表
     * @param customObservationConvention 自定义观测约定
     * @param batchingStrategy 批处理策略
     * @return 向量存储
     */
    @Bean
    @Primary
    public VectorStore aiVectorStore(EmbeddingModel embeddingModel,
                                     JedisConnectionFactory jedisConnectionFactory,
                                     RedisVectorStoreProperties properties,
                                     ObjectProvider<ObservationRegistry> observationRegistry,
                                     ObjectProvider<VectorStoreObservationConvention> customObservationConvention,
                                     BatchingStrategy batchingStrategy) {
        return RedisVectorStore.builder(createJedisPooled(jedisConnectionFactory), embeddingModel)
                .initializeSchema(properties.isInitializeSchema())
                .observationRegistry(observationRegistry.getIfUnique(() -> ObservationRegistry.NOOP))
                .customObservationConvention(customObservationConvention.getIfAvailable(() -> null))
                .batchingStrategy(batchingStrategy)
                .indexName(properties.getIndexName())
                .prefix(properties.getPrefix())
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

    private JedisPooled createJedisPooled(JedisConnectionFactory jedisConnectionFactory) {
        JedisClientConfig clientConfig = DefaultJedisClientConfig.builder()
                .ssl(jedisConnectionFactory.isUseSsl())
                .clientName(jedisConnectionFactory.getClientName())
                .timeoutMillis(jedisConnectionFactory.getTimeout())
                .password(jedisConnectionFactory.getPassword())
                .build();
        return new JedisPooled(
                new HostAndPort(jedisConnectionFactory.getHostName(), jedisConnectionFactory.getPort()),
                clientConfig
        );
    }
}

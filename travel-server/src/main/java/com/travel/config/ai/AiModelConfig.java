package com.travel.config.ai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
}

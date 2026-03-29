package com.travel.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 配置。
 * <p>
 * 统一定义 JSON 序列化器、通用 RedisTemplate 和字符串模板。
 */
@Configuration
public class RedisConfig {

    /**
     * 注册 Redis JSON 序列化器。
     *
     * @return 通用 JSON 序列化器
     */
    @Bean
    public GenericJackson2JsonRedisSerializer redisJsonSerializer() {
        return new GenericJackson2JsonRedisSerializer();
    }

    /**
     * 注册通用 RedisTemplate，并统一 Key 与 Value 的序列化策略。
     *
     * @param connectionFactory Redis 连接工厂
     * @param redisJsonSerializer JSON 序列化器
     * @return 通用 RedisTemplate
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(
        RedisConnectionFactory connectionFactory,
        GenericJackson2JsonRedisSerializer redisJsonSerializer
    ) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Key 统一使用字符串序列化。
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setDefaultSerializer(redisJsonSerializer);

        // Value 统一使用 JSON 序列化。
        template.setValueSerializer(redisJsonSerializer);
        template.setHashValueSerializer(redisJsonSerializer);

        template.afterPropertiesSet();
        return template;
    }

    /**
     * 注册字符串专用的 RedisTemplate。
     *
     * @param connectionFactory Redis 连接工厂
     * @return StringRedisTemplate 实例
     */
    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }
}

package com.travel.service.ai.rag;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.task.TaskExecutor;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.util.ErrorHandler;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * AI 知识任务 Stream 消费者。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiKnowledgeImportJobConsumer {

    private final RedisConnectionFactory redisConnectionFactory;
    private final org.springframework.data.redis.core.StringRedisTemplate stringRedisTemplate;
    private final AiKnowledgeImportJobProcessor aiKnowledgeImportJobProcessor;
    private final TaskExecutor aiKnowledgeJobExecutor;

    @org.springframework.beans.factory.annotation.Qualifier("aiKnowledgeStreamExecutor")
    private final TaskExecutor aiKnowledgeStreamExecutor;

    private StreamMessageListenerContainer<String, MapRecord<String, String, String>> container;
    private final AtomicBoolean shuttingDown = new AtomicBoolean(false);

    @PostConstruct
    public void start() {
        ensureGroup();
        StreamMessageListenerContainer.StreamMessageListenerContainerOptions<String, MapRecord<String, String, String>> options =
                StreamMessageListenerContainer.StreamMessageListenerContainerOptions.builder()
                        .pollTimeout(Duration.ofSeconds(1))
                        .executor(aiKnowledgeStreamExecutor)
                        .errorHandler(buildErrorHandler())
                        .build();
        container = StreamMessageListenerContainer.create(redisConnectionFactory, options);
        container.receiveAutoAck(
                Consumer.from(AiKnowledgeJobStreamSupport.GROUP, AiKnowledgeJobStreamSupport.CONSUMER),
                StreamOffset.create(AiKnowledgeJobStreamSupport.STREAM_KEY, ReadOffset.lastConsumed()),
                this::handleRecord
        );
        container.start();
    }

    @PreDestroy
    public void stop() {
        if (container != null) {
            shuttingDown.set(true);
            CountDownLatch latch = new CountDownLatch(1);
            try {
                container.stop(latch::countDown);
                if (!latch.await(3, TimeUnit.SECONDS)) {
                    log.warn("AI 知识任务 Stream 监听容器停止超时，继续执行关闭流程");
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                log.warn("AI 知识任务 Stream 监听容器停止被中断");
            } catch (Exception exception) {
                log.warn("AI 知识任务 Stream 监听容器关闭异常：{}", exception.getMessage());
            }
        }
    }

    private void ensureGroup() {
        try {
            if (!Boolean.TRUE.equals(stringRedisTemplate.hasKey(AiKnowledgeJobStreamSupport.STREAM_KEY))) {
                stringRedisTemplate.opsForStream().add(
                        AiKnowledgeJobStreamSupport.STREAM_KEY,
                        java.util.Map.of(
                                AiKnowledgeJobStreamSupport.FIELD_TYPE, "BOOTSTRAP",
                                AiKnowledgeJobStreamSupport.FIELD_DOCUMENT_ID, "0"
                        )
                );
            }
            stringRedisTemplate.opsForStream().createGroup(
                    AiKnowledgeJobStreamSupport.STREAM_KEY,
                    ReadOffset.latest(),
                    AiKnowledgeJobStreamSupport.GROUP
            );
        } catch (Exception ignored) {
            // 组已存在或流已初始化时直接复用即可。
        }
    }

    private void handleRecord(MapRecord<String, String, String> record) {
        String type = record.getValue().get(AiKnowledgeJobStreamSupport.FIELD_TYPE);
        String documentId = record.getValue().get(AiKnowledgeJobStreamSupport.FIELD_DOCUMENT_ID);
        if (!AiKnowledgeJobStreamSupport.TYPE_REBUILD_DOCUMENT.equals(type) || !StringUtils.hasText(documentId)) {
            return;
        }
        aiKnowledgeJobExecutor.execute(() -> aiKnowledgeImportJobProcessor.processDocument(Long.parseLong(documentId)));
    }

    /**
     * 构建 Stream 监听错误处理器。
     * <p>
     * 应用关闭阶段 Redis 连接会先后回收，此时的 Connection closed 属于预期噪声，不应再打 error。
     *
     * @return 错误处理器
     */
    private ErrorHandler buildErrorHandler() {
        return throwable -> {
            String message = throwable == null ? "" : throwable.getMessage();
            if (shuttingDown.get() || (message != null && message.contains("Connection closed"))) {
                log.info("AI 知识任务 Stream 监听已在关闭阶段停止：{}", message);
                return;
            }
            log.error("AI 知识任务 Stream 监听异常", throwable);
        };
    }
}

package com.travel.service.ai.chat;

import com.travel.common.exception.BusinessException;
import com.travel.common.result.ResultCode;
import com.travel.config.ai.AiProperties;
import com.travel.dto.ai.request.AiChatMessageRequest;
import com.travel.enums.ai.AiScenarioType;
import com.travel.service.ai.guardrail.AiGuardrailService;
import com.travel.service.ai.intent.AiIntentService;
import com.travel.service.ai.memory.AiSessionIdService;
import com.travel.service.ai.memory.RedisChatMemory;
import com.travel.service.ai.rag.AiKnowledgeContextAdvisor;
import com.travel.service.ai.rag.AiKnowledgeRetrievalService;
import com.travel.service.ai.tool.AiToolContextHolder;
import com.travel.service.ai.tool.AiToolExecutionService;
import com.travel.service.ai.tool.AiToolRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.retry.NonTransientAiException;
import org.springframework.core.task.TaskExecutor;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * AI 对话编排主链路测试。
 */
class AiConversationServiceTest {

    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();

    @AfterEach
    void tearDown() {
        meterRegistry.clear();
        meterRegistry.close();
    }

    @Test
    void streamChatCompletesWithErrorWhenGuardrailThrowsBusinessException() throws Exception {
        TestFixture fixture = createFixture();
        doThrow(new BusinessException(ResultCode.PARAM_ERROR, "请求过于频繁，请稍后重试"))
                .when(fixture.aiGuardrailService)
                .enforceRateLimit(anyString(), anyString());

        SseEmitter emitter = spy(new SseEmitter(0L));
        doNothing().when(emitter).send(any(SseEmitter.SseEventBuilder.class));

        assertDoesNotThrow(() -> invokeStreamChat(fixture.service, emitter, fixture.request(), 1L, null, "127.0.0.1"));

        verify(emitter, times(1)).send(any(SseEmitter.SseEventBuilder.class));
        verify(emitter, times(1)).complete();
        assertEquals(1D, counterValue("waytrip.ai.chat.requests.total"));
        assertEquals(1D, counterValue("waytrip.ai.chat.requests.completed", "scenario", "UNKNOWN", "outcome", "business_error"));
    }

    @Test
    void streamChatKeepsGeneratingWhenRetrievalTrackTimesOut() throws Exception {
        TestFixture fixture = createFixture();
        when(fixture.aiKnowledgeRetrievalService.retrieve(eq(AiScenarioType.TRAVEL_PLANNER), anyString()))
                .thenThrow(new CompletionException(new TimeoutException("retrieval timeout")));

        SseEmitter emitter = spy(new SseEmitter(0L));
        doNothing().when(emitter).send(any(SseEmitter.SseEventBuilder.class));

        assertDoesNotThrow(() -> invokeStreamChat(fixture.service, emitter, fixture.request(), 1L, null, "127.0.0.1"));

        verify(emitter, times(3)).send(any(SseEmitter.SseEventBuilder.class));
        verify(emitter, times(1)).complete();
        assertEquals(1D, counterValue("waytrip.ai.chat.parallel.degraded",
                "stage", "retrieval",
                "scenario", "TRAVEL_PLANNER",
                "failureType", "timeout"));
        assertEquals(1D, counterValue("waytrip.ai.chat.requests.completed",
                "scenario", "TRAVEL_PLANNER",
                "outcome", "success"));
    }

    @Test
    void streamChatKeepsGeneratingWhenToolingTrackFails() throws Exception {
        TestFixture fixture = createFixture();
        when(fixture.aiToolExecutionService.execute(eq(AiScenarioType.TRAVEL_PLANNER), any()))
                .thenThrow(new BusinessException(ResultCode.ACCESS_DENIED, "当前 AI 工具需要登录后使用"));

        SseEmitter emitter = spy(new SseEmitter(0L));
        doNothing().when(emitter).send(any(SseEmitter.SseEventBuilder.class));

        assertDoesNotThrow(() -> invokeStreamChat(fixture.service, emitter, fixture.request(), 1L, null, "127.0.0.1"));

        verify(emitter, times(3)).send(any(SseEmitter.SseEventBuilder.class));
        verify(emitter, times(1)).complete();
        assertEquals(1D, counterValue("waytrip.ai.chat.parallel.degraded",
                "stage", "tooling",
                "scenario", "TRAVEL_PLANNER",
                "failureType", "business_error"));
        assertEquals(1D, counterValue("waytrip.ai.chat.requests.completed",
                "scenario", "TRAVEL_PLANNER",
                "outcome", "success"));
    }

    @Test
    void streamChatRecordsModelErrorWhenGenerationFails() throws Exception {
        TestFixture fixture = createFixture();
        when(fixture.streamResponseSpec.content()).thenReturn(Flux.error(new NonTransientAiException("model not found")));

        SseEmitter emitter = spy(new SseEmitter(0L));
        doNothing().when(emitter).send(any(SseEmitter.SseEventBuilder.class));

        assertDoesNotThrow(() -> invokeStreamChat(fixture.service, emitter, fixture.request(), 1L, null, "127.0.0.1"));

        verify(emitter, times(2)).send(any(SseEmitter.SseEventBuilder.class));
        verify(emitter, times(1)).complete();
        assertEquals(1D, counterValue("waytrip.ai.chat.requests.completed",
                "scenario", "TRAVEL_PLANNER",
                "outcome", "model_error"));
    }

    private void invokeStreamChat(AiConversationService service,
                                  SseEmitter emitter,
                                  AiChatMessageRequest request,
                                  Long userId,
                                  Long adminId,
                                  String clientIp) throws Exception {
        Method method = AiConversationService.class.getDeclaredMethod(
                "streamChat",
                SseEmitter.class,
                AiChatMessageRequest.class,
                Long.class,
                Long.class,
                String.class
        );
        method.setAccessible(true);
        method.invoke(service, emitter, request, userId, adminId, clientIp);
    }

    private double counterValue(String name, String... tags) {
        return meterRegistry.find(name).tags(tags).counter().count();
    }

    private TestFixture createFixture() {
        ChatClient aiChatClient = mock(ChatClient.class);
        ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.StreamResponseSpec streamResponseSpec = mock(ChatClient.StreamResponseSpec.class);
        RedisChatMemory redisChatMemory = mock(RedisChatMemory.class);
        AiSessionIdService aiSessionIdService = mock(AiSessionIdService.class);
        AiGuardrailService aiGuardrailService = mock(AiGuardrailService.class);
        AiScenarioRouter aiScenarioRouter = mock(AiScenarioRouter.class);
        AiPromptManager aiPromptManager = mock(AiPromptManager.class);
        AiIntentService aiIntentService = mock(AiIntentService.class);
        AiConversationContextService aiConversationContextService = mock(AiConversationContextService.class);
        AiKnowledgeRetrievalService aiKnowledgeRetrievalService = mock(AiKnowledgeRetrievalService.class);
        AiKnowledgeContextAdvisor aiKnowledgeContextAdvisor = mock(AiKnowledgeContextAdvisor.class);
        AiToolRegistry aiToolRegistry = mock(AiToolRegistry.class);
        AiToolExecutionService aiToolExecutionService = mock(AiToolExecutionService.class);
        AiToolContextHolder aiToolContextHolder = new AiToolContextHolder();
        AiResponseAssembler aiResponseAssembler = new AiResponseAssembler();
        AiContextFusionService aiContextFusionService = new AiContextFusionService();
        TaskExecutor directExecutor = Runnable::run;

        AiProperties aiProperties = new AiProperties();
        aiProperties.getMetrics().setEnabled(Boolean.TRUE);
        aiProperties.getChat().setParallelTimeoutMillis(100);

        when(aiSessionIdService.normalizeSessionId(anyString())).thenReturn("session-1");
        when(aiSessionIdService.createSessionId()).thenReturn("message-1");
        when(aiGuardrailService.sanitizeUserMessage(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
        when(aiScenarioRouter.route(anyString(), anyString(), anyString())).thenReturn(AiScenarioType.TRAVEL_PLANNER);
        when(aiPromptManager.buildSystemPrompt(AiScenarioType.TRAVEL_PLANNER)).thenReturn("你是 WayTrip 旅游助手。");
        when(aiConversationContextService.assemble(eq(1L), eq(AiScenarioType.TRAVEL_PLANNER), any()))
                .thenReturn(AiConversationContext.empty());
        when(aiKnowledgeRetrievalService.retrieve(eq(AiScenarioType.TRAVEL_PLANNER), anyString())).thenReturn(List.of());
        when(aiToolExecutionService.execute(eq(AiScenarioType.TRAVEL_PLANNER), any()))
                .thenReturn(AiToolExecutionService.AiToolExecutionResult.empty());
        when(aiToolRegistry.resolveTools(AiScenarioType.TRAVEL_PLANNER)).thenReturn(new Object[0]);

        when(aiChatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.advisors(Mockito.<java.util.function.Consumer<ChatClient.AdvisorSpec>>any())).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.stream()).thenReturn(streamResponseSpec);
        when(streamResponseSpec.content()).thenReturn(Flux.just("你好，下面给你一个轻松两日游建议。"));

        AiConversationService service = new AiConversationService(
                aiChatClient,
                aiProperties,
                meterRegistry,
                redisChatMemory,
                aiSessionIdService,
                aiGuardrailService,
                aiScenarioRouter,
                aiPromptManager,
                aiIntentService,
                aiResponseAssembler,
                aiContextFusionService,
                aiConversationContextService,
                aiKnowledgeRetrievalService,
                aiKnowledgeContextAdvisor,
                aiToolContextHolder,
                aiToolRegistry,
                aiToolExecutionService,
                directExecutor,
                directExecutor
        );
        return new TestFixture(
                service,
                aiGuardrailService,
                aiKnowledgeRetrievalService,
                aiToolExecutionService,
                streamResponseSpec
        );
    }

    private record TestFixture(
            AiConversationService service,
            AiGuardrailService aiGuardrailService,
            AiKnowledgeRetrievalService aiKnowledgeRetrievalService,
            AiToolExecutionService aiToolExecutionService,
            ChatClient.StreamResponseSpec streamResponseSpec
    ) {

        private AiChatMessageRequest request() {
            AiChatMessageRequest request = new AiChatMessageRequest();
            request.setSessionId("session-1");
            request.setMessage("最近想带爸妈轻松玩两天，有什么建议");
            request.setScenarioHint("TRAVEL_PLANNER");
            request.setSourcePage("Recommendations");
            return request;
        }
    }
}

package com.travel.service.ai.chat;

import com.travel.enums.ai.AiScenarioType;
import com.travel.service.ai.intent.AiScenarioFallback;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * AI 场景路由器，负责将请求简单归类到核心场景。
 */
@Component
@RequiredArgsConstructor
public class AiScenarioRouter {

    /**
     * 全局规则兜底路由。
     */
    private final AiScenarioFallback scenarioFallback;

    /**
     * 根据显式提示、来源页面和消息关键词推断当前对话场景。
     * <p>
     * 判定顺序遵循“前端显式 hint > 页面来源 > 用户文本”，避免模型链路在已知场景下再次猜测。
     *
     * @param userMessage 用户消息
     * @param scenarioHint 前端显式场景提示
     * @param sourcePage 来源页面标识
     * @return 推断后的场景类型
     */
    public AiScenarioType route(String userMessage, String scenarioHint, String sourcePage) {
        AiScenarioType hintedScenario = resolveScenarioHint(scenarioHint);
        if (hintedScenario != null) {
            return hintedScenario;
        }
        return scenarioFallback.route(userMessage, scenarioHint, sourcePage);
    }

    /**
     * 解析前端传入的场景提示，只接受正式场景枚举名。
     *
     * @param scenarioHint 原始场景提示
     * @return 场景类型；无法识别时返回 null
     */
    private AiScenarioType resolveScenarioHint(String scenarioHint) {
        if (!StringUtils.hasText(scenarioHint)) {
            return null;
        }
        String normalized = scenarioHint.trim().toUpperCase();
        try {
            return AiScenarioType.valueOf(normalized);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}

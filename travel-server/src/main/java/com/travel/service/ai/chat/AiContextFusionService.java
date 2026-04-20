package com.travel.service.ai.chat;

import com.travel.service.ai.intent.AiIntentResult;
import com.travel.service.ai.tool.AiToolExecutionService.AiToolExecutionResult;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * AI 上下文融合服务。
 *
 * <p>统一收口系统提示词、业务上下文、意图提示和右轨预执行结果，
 * 避免主链路继续通过多个私有方法手工拼接系统提示词。</p>
 */
@Service
public class AiContextFusionService {

    /**
     * 融合系统提示词与多路上下文，生成最终给模型使用的系统提示词。
     *
     * @param systemPrompt 基础系统提示词
     * @param conversationContext 业务上下文
     * @param intentResult 意图识别结果
     * @param toolExecutionResult 右轨预执行工具结果
     * @return 融合后的系统提示词
     */
    public String fuse(String systemPrompt,
                       AiConversationContext conversationContext,
                       AiIntentResult intentResult,
                       AiToolExecutionResult toolExecutionResult) {
        StringBuilder builder = new StringBuilder(systemPrompt == null ? "" : systemPrompt.trim());

        appendConversationContext(builder, conversationContext);
        appendIntentHint(builder, intentResult);
        appendPreExecutedToolResult(builder, toolExecutionResult);

        return builder.toString();
    }

    /**
     * 追加业务上下文，帮助模型优先利用系统已知事实理解问题。
     *
     * @param builder 提示词构建器
     * @param conversationContext 对话上下文
     */
    private void appendConversationContext(StringBuilder builder, AiConversationContext conversationContext) {
        if (conversationContext == null || conversationContext.isEmpty()) {
            return;
        }
        builder.append(conversationContext.promptText());
    }

    /**
     * 追加意图和槽位提示，帮助模型优先围绕真实任务调用工具。
     *
     * @param builder 提示词构建器
     * @param intentResult 意图识别结果
     */
    private void appendIntentHint(StringBuilder builder, AiIntentResult intentResult) {
        if (intentResult == null || !StringUtils.hasText(intentResult.intent()) || intentResult.intent().endsWith("NONE")) {
            return;
        }
        builder.append("\n\n已识别的用户意图：").append(intentResult.intent());
        if (intentResult.slots() != null && !intentResult.slots().isEmpty()) {
            builder.append("\n已提取条件：").append(intentResult.slots());
        }
        builder.append("\n请优先围绕该意图调用最匹配的工具，再组织回复。");
    }

    /**
     * 追加右轨预执行工具结果，优先把已拿到的真实业务数据交给最终模型。
     *
     * @param builder 提示词构建器
     * @param toolExecutionResult 工具预执行结果
     */
    private void appendPreExecutedToolResult(StringBuilder builder, AiToolExecutionResult toolExecutionResult) {
        if (toolExecutionResult == null || !toolExecutionResult.executed() || toolExecutionResult.payload() == null) {
            return;
        }
        builder.append("\n\n系统已预执行右轨工具：").append(toolExecutionResult.toolName())
                .append("\n已获得的结构化数据如下，请优先使用这些真实数据组织回答；只有当这些数据不足时，再决定是否补充调用其它工具：")
                .append("\n").append(toolExecutionResult.payload());
    }
}

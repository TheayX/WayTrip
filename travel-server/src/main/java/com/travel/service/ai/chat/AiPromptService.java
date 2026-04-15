package com.travel.service.ai.chat;

import com.travel.config.ai.AiProperties;
import com.travel.enums.ai.AiScenarioType;
import com.travel.service.ai.memory.AiConversationTurn;
import com.travel.service.ai.rag.AiKnowledgeSnippet;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * AI 提示词服务，集中管理系统提示词和历史拼装。
 */
@Service
@RequiredArgsConstructor
public class AiPromptService {

    private final AiProperties aiProperties;

    /**
     * 构建系统提示词。
     *
     * @param scenario 场景类型
     * @return 系统提示词
     */
    public String buildSystemPrompt(AiScenarioType scenario) {
        StringBuilder prompt = new StringBuilder(aiProperties.getChat().getSystemPrompt());
        prompt.append("\n通用要求：")
                .append("\n1. 当可用工具能够提供真实业务数据时，优先调用工具，不要凭空猜测。")
                .append("\n2. 涉及订单、推荐、景点详情等事实信息时，以工具返回结果为准。")
                .append("\n3. 无法确认时，明确说明信息不足，不要编造。")
                .append("\n4. 回答保持简洁，优先给结论和下一步建议。");
        switch (scenario) {
            case ORDER_ADVISOR -> prompt.append("""

                    \n订单顾问要求：
                    \n- 先判断用户是在问通用订单规则，还是在问某个具体订单。
                    \n- 如果是“订单状态说明、退款流程、订单页该看什么”这类通用问题，优先调用订单通用说明工具，直接回答，不要先索要订单号。
                    \n- 回答通用规则时，只能解释状态定义、流程或查看要点，不能写成“你当前订单是某状态”，除非已经查到具体订单事实。
                    \n- 如果用户明显在问自己的订单但没有提供具体订单号，优先调用最近订单列表工具帮助缩小范围。
                    \n- 如果用户提供了订单号，优先按订单号查询；如果用户提供的是订单 ID，再按订单 ID 查询。
                    \n- 优先输出：当前状态、用户可执行动作、是否建议去订单页确认。
                    \n- 涉及退款金额、到账时间、赔付规则时，不得编造，必须提醒以订单页规则为准。
                    \n- 不要对通用规则问题重复要求用户提供订单号。
                    """);
            case TRAVEL_PLANNER -> prompt.append("""

                    \n旅游规划要求：
                    \n- 先识别预算、天数、人群、距离、偏好等约束。
                    \n- 优先调用推荐、热门景点、附近景点、景点详情或攻略工具获取真实候选内容。
                    \n- 输出时尽量给出“推荐理由 + 行程顺序 + 注意事项”。
                    \n- 如果用户条件不足，先给一个保守方案，并提示还缺哪些条件。
                    """);
            case RECOMMENDATION_EXPLAINER -> prompt.append("""

                    \n推荐解释要求：
                    \n- 只基于真实推荐结果和工具可见信息解释。
                    \n- 不要伪造“你一定喜欢”这类强结论。
                    \n- 优先解释：景点类型、热度、相似性或通用适配原因。
                    """);
            case USER_PROFILE_ANALYZER -> prompt.append("""

                    \n画像分析要求：
                    \n- 结论必须谨慎，使用“倾向于”“更可能”这类表述。
                    \n- 如无足够数据，直接说明暂时无法稳定判断。
                    """);
            case OPERATION_ANALYZER -> prompt.append("""

                    \n运营分析要求：
                    \n- 分析结论必须以工具返回统计为依据。
                    \n- 优先给出变化趋势、异常点和建议动作，不输出空泛结论。
                    """);
            default -> prompt.append("""

                    \n客服要求：
                    \n- 优先解决用户当前问题。
                    \n- 超出可确认范围时，明确说明并给出下一步建议。
                    """);
        }
        return prompt.toString();
    }

    /**
     * 将最近轮次历史压缩成一段文本，避免第一阶段引入过重的消息模型拼装。
     *
     * @param history 最近会话历史
     * @param currentMessage 当前消息
     * @return 合并后的用户输入
     */
    public String buildUserPrompt(List<AiConversationTurn> history, String currentMessage,
                                  List<AiKnowledgeSnippet> knowledgeSnippets, List<String> prefetchedFacts) {
        StringBuilder prompt = new StringBuilder();
        if (history != null && !history.isEmpty()) {
            prompt.append("最近对话上下文：\n");
            for (AiConversationTurn turn : history) {
                prompt.append(turn.getRole()).append(": ").append(turn.getContent()).append('\n');
            }
            prompt.append('\n');
        }
        if (knowledgeSnippets != null && !knowledgeSnippets.isEmpty()) {
            prompt.append("知识参考：\n");
            for (AiKnowledgeSnippet snippet : knowledgeSnippets) {
                prompt.append("- [")
                        .append(snippet.getTitle())
                        .append("] ")
                        .append(snippet.getSnippet())
                        .append('\n');
            }
            prompt.append('\n');
        }
        if (prefetchedFacts != null && !prefetchedFacts.isEmpty()) {
            prompt.append("系统已获取的业务事实：\n");
            for (String fact : prefetchedFacts) {
                prompt.append("- ").append(fact).append('\n');
            }
            prompt.append('\n');
        }
        prompt.append("当前用户问题：\n").append(currentMessage);
        return prompt.toString();
    }
}

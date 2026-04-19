package com.travel.service.ai.chat;

import com.travel.config.ai.AiProperties;
import com.travel.enums.ai.AiScenarioType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * AI 提示词管理器，集中管理系统提示词。
 */
@Service
@RequiredArgsConstructor
public class AiPromptManager {

    /**
     * AI 配置，提供系统提示词基础模板。
     */
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
                .append("\n1. 当可用工具能补充真实数据时，优先调用工具，再结合结果自然回答。")
                .append("\n2. 所有涉及个人的查询（我的订单、我的收藏、推荐），工具已自动绑定上下文用户，不要向用户索要账号、登录名或ID。")
                .append("\n3. 工具的非必填参数（如状态、偏好等）如果用户没说，直接按当前已知信息调用，不要为了可选条件打断用户。")
                .append("\n4. 涉及订单、推荐、景点详情等事实信息时，以工具返回结果为准；无法确认具体事实时，可以先给保守判断，再说明仍需补充或以页面为准。")
                .append("\n5. 回答保持自然、简洁，优先给用户当前能用的结论、建议或下一步。");
        // 场景规则只补充“当前任务额外约束”，通用原则统一放在前面，避免提示词重复堆叠。
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
                    \n- 如果用户条件不足，先给一个大致可用的保守方案，再顺手提示补充哪些条件能进一步收敛。
                    """);
            case SPOT_QA -> prompt.append("""

                    \n景点问答要求：
                    \n- 优先基于系统景点工具结果回答价格、开放时间、地址、评分和简介。
                    \n- 未查询到景点时，不要编造景点事实；可以先给相近方向的说明，再引导用户补充更明确的景点名或关键词。
                    \n- 价格和开放时间必须提示以页面展示为准。
                    """);
            case GUIDE_QA -> prompt.append("""

                    \n攻略问答要求：
                    \n- 优先基于系统攻略摘要和知识库内容回答怎么玩、避坑点和推荐阅读方向。
                    \n- 不得伪造不存在的攻略来源。
                    """);
            case RECOMMENDATION_EXPLAINER -> prompt.append("""

                    \n推荐解释要求：
                    \n- 只基于真实推荐结果和工具可见信息解释。
                    \n- 不要伪造“你一定喜欢”这类强结论。
                    \n- 优先解释：景点类型、热度、相似性或通用适配原因；信息不完整时也先给合理解释框架。
                    """);
            case USER_PROFILE_ANALYZER -> prompt.append("""

                    \n用户画像要求：
                    \n- 只能基于当前用户真实浏览、收藏、订单、评价和偏好数据总结。
                    \n- 数据不足时必须明确说明，不要只靠对话内容推断偏好。
                    \n- 结论使用“倾向于”“更可能”这类表述，不要输出绝对判断。
                    """);
            case OPERATION_ANALYZER -> prompt.append("""

                    \n运营分析要求：
                    \n- 只能基于管理端统计工具返回的数据总结。
                    \n- 不得编造转化率、留存率等当前统计接口没有提供的指标。
                    \n- 运营数据需要管理端权限，未授权时不要输出任何后台统计。
                    """);
            default -> prompt.append("""

                    \n客服要求：
                    \n- 优先解决用户当前问题。
                    \n- 超出可确认范围时，先给用户可参考的方向，再说明边界和下一步建议。
                    """);
        }
        return prompt.toString();
    }
}

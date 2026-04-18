package com.travel.service.ai.tool;

import com.travel.enums.ai.AiScenarioType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * AI 工具注册中心，统一维护工具集合与适用场景，避免在编排器中硬编码 switch 映射。
 */
@Component
@RequiredArgsConstructor
public class AiToolRegistry {

    /**
     * 推荐相关工具集合。
     */
    private final RecommendationAiTools recommendationAiTools;

    /**
     * 订单相关工具集合。
     */
    private final OrderAiTools orderAiTools;

    /**
     * 景点与攻略相关工具集合。
     */
    private final SpotAiTools spotAiTools;

    /**
     * 用户画像相关工具集合。
     */
    private final UserProfileAiTools userProfileAiTools;

    /**
     * 管理端运营分析工具集合。
     */
    private final OperationAiTools operationAiTools;

    /**
     * 根据场景解析当前可暴露的工具集合。
     *
     * @param scenario 场景类型
     * @return 工具对象数组
     */
    public Object[] resolveTools(AiScenarioType scenario) {
        if (scenario == null || scenario == AiScenarioType.CUSTOMER_SERVICE) {
            return new Object[0];
        }
        Set<Object> tools = new LinkedHashSet<>();
        for (ToolBinding binding : bindings()) {
            if (binding.getScenarios().contains(scenario)) {
                tools.add(binding.getTool());
            }
        }
        return tools.toArray();
    }

    /**
     * 定义工具与场景的静态绑定关系。
     *
     * @return 绑定列表
     */
    private List<ToolBinding> bindings() {
        List<ToolBinding> bindings = new ArrayList<>();
        bindings.add(new ToolBinding(orderAiTools, Set.of(AiScenarioType.ORDER_ADVISOR)));
        bindings.add(new ToolBinding(spotAiTools, Set.of(
                AiScenarioType.ORDER_ADVISOR,
                AiScenarioType.SPOT_QA,
                AiScenarioType.GUIDE_QA,
                AiScenarioType.TRAVEL_PLANNER,
                AiScenarioType.RECOMMENDATION_EXPLAINER,
                AiScenarioType.CUSTOMER_SERVICE
        )));
        bindings.add(new ToolBinding(recommendationAiTools, Set.of(
                AiScenarioType.TRAVEL_PLANNER,
                AiScenarioType.RECOMMENDATION_EXPLAINER
        )));
        bindings.add(new ToolBinding(userProfileAiTools, Set.of(AiScenarioType.USER_PROFILE_ANALYZER)));
        bindings.add(new ToolBinding(operationAiTools, Set.of(AiScenarioType.OPERATION_ANALYZER)));
        return bindings;
    }

    @Getter
    @RequiredArgsConstructor
    private static class ToolBinding {

        /**
         * Spring AI 可注册的工具实例。
         */
        private final Object tool;

        /**
         * 当前工具允许暴露的场景集合。
         */
        private final Set<AiScenarioType> scenarios;
    }
}

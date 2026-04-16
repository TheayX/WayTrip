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

    private final RecommendationAiTools recommendationAiTools;
    private final OrderAiTools orderAiTools;
    private final SpotAiTools spotAiTools;
    private final UserProfileAiTools userProfileAiTools;
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
        private final Object tool;
        private final Set<AiScenarioType> scenarios;
    }
}

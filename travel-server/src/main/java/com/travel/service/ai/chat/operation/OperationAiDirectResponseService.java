package com.travel.service.ai.chat.operation;

import com.travel.service.ai.chat.intent.AiIntentClassificationResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 运营分析 AI 直答服务。
 */
@Service
@RequiredArgsConstructor
public class OperationAiDirectResponseService {

    private final OperationAiIntentResolver operationAiIntentResolver;
    private final OperationAiToolPolicy operationAiToolPolicy;
    private final OperationAiResponseComposer operationAiResponseComposer;

    /**
     * 尝试生成运营分析确定性回复。
     *
     * @param userMessage 用户问题
     * @return 可直接返回的回复；无法识别时返回空字符串
     */
    public String tryReply(String userMessage) {
        if (!StringUtils.hasText(userMessage)) {
            return "";
        }
        AiIntentClassificationResult intentResult = operationAiIntentResolver.resolve(userMessage);
        OperationAiIntent intent = parseIntent(intentResult.intent());
        if (intent == OperationAiIntent.NONE) {
            return "";
        }
        return operationAiResponseComposer.compose(operationAiToolPolicy.execute(intent, intentResult));
    }

    private OperationAiIntent parseIntent(String value) {
        if (!StringUtils.hasText(value)) {
            return OperationAiIntent.NONE;
        }
        try {
            return OperationAiIntent.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return OperationAiIntent.NONE;
        }
    }
}

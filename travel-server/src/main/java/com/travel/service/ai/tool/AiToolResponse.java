package com.travel.service.ai.tool;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * AI 工具响应封装，统一组织成功与失败语义，便于模型稳定理解工具结果。
 */
public final class AiToolResponse {

    private AiToolResponse() {
    }

    /**
     * 返回成功的工具结果。
     *
     * @param message 简短说明
     * @param data 业务数据
     * @return 结构化响应
     */
    public static Map<String, Object> success(String message, Object data) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("message", message);
        result.put("data", data);
        return result;
    }

    /**
     * 返回可预期失败结果，例如未查到数据。
     *
     * @param message 失败说明
     * @return 结构化响应
     */
    public static Map<String, Object> failure(String message) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", false);
        result.put("message", message);
        return result;
    }
}

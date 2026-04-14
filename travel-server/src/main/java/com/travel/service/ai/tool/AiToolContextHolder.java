package com.travel.service.ai.tool;

import com.travel.common.exception.BusinessException;
import com.travel.common.result.ResultCode;
import com.travel.dto.ai.response.AiToolCallItem;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * AI 工具上下文持有器，统一保存当前请求的用户身份。
 */
@Component
public class AiToolContextHolder {

    private static final ThreadLocal<Long> CURRENT_USER_ID = new ThreadLocal<>();
    private static final ThreadLocal<List<AiToolCallItem>> TOOL_TRACES = ThreadLocal.withInitial(ArrayList::new);

    /**
     * 绑定当前请求用户 ID。
     *
     * @param userId 用户 ID，可为空
     */
    public void setCurrentUserId(Long userId) {
        CURRENT_USER_ID.set(userId);
    }

    /**
     * 获取当前请求用户 ID。
     *
     * @return 用户 ID，可为空
     */
    public Long getCurrentUserId() {
        return CURRENT_USER_ID.get();
    }

    /**
     * 获取当前已登录用户 ID，不存在时抛出异常。
     *
     * @return 用户 ID
     */
    public Long requireCurrentUserId() {
        Long userId = CURRENT_USER_ID.get();
        if (userId == null) {
            throw new BusinessException(ResultCode.ACCESS_DENIED, "当前 AI 工具需要登录后使用");
        }
        return userId;
    }

    /**
     * 记录工具调用摘要。
     *
     * @param toolName 工具名
     * @param toolCategory 工具分类
     * @param success 是否成功
     * @param summary 摘要说明
     */
    public void addToolTrace(String toolName, String toolCategory, boolean success, String summary) {
        TOOL_TRACES.get().add(new AiToolCallItem(toolName, toolCategory, success, summary));
    }

    /**
     * 获取当前请求期间的工具调用摘要。
     *
     * @return 工具摘要列表
     */
    public List<AiToolCallItem> getToolTraces() {
        return new ArrayList<>(TOOL_TRACES.get());
    }

    /**
     * 清理当前请求上下文。
     */
    public void clear() {
        CURRENT_USER_ID.remove();
        TOOL_TRACES.remove();
    }
}

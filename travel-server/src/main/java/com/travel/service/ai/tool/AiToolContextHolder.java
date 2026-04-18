package com.travel.service.ai.tool;

import com.travel.common.exception.BusinessException;
import com.travel.common.result.ResultCode;
import com.travel.dto.ai.response.AiToolCallItem;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;

/**
 * AI 工具上下文持有器，统一保存当前请求的身份信息和工具调用轨迹。
 * <p>
 * 工具调用链路在同步与流式输出场景下都需要读取同一份请求态数据，
 * 因此这里将状态收口为单个请求上下文对象，避免调用轨迹散落在多个 ThreadLocal 字段中。
 */
@Component
public class AiToolContextHolder {

    /**
     * 当前执行链路绑定的请求上下文。
     */
    private static final ThreadLocal<AiToolRequestContext> CURRENT_CONTEXT = new ThreadLocal<>();

    /**
     * 在指定请求上下文中执行代码块。
     *
     * @param context 请求上下文
     * @param action 执行逻辑
     * @param <T> 返回值类型
     * @return 执行结果
     */
    public <T> T withContext(AiToolRequestContext context, Supplier<T> action) {
        CURRENT_CONTEXT.set(context);
        try {
            return action.get();
        } finally {
            CURRENT_CONTEXT.remove();
        }
    }

    /**
     * 在指定请求上下文中执行无返回值代码块。
     *
     * @param context 请求上下文
     * @param action 执行逻辑
     */
    public void withContext(AiToolRequestContext context, Runnable action) {
        withContext(context, () -> {
            action.run();
            return null;
        });
    }

    /**
     * 获取当前请求用户 ID。
     *
     * @return 用户 ID，可为空
     */
    public Long getCurrentUserId() {
        return requireContext().getUserId();
    }

    /**
     * 获取当前请求管理员 ID。
     *
     * @return 管理员 ID，可为空
     */
    public Long getCurrentAdminId() {
        return requireContext().getAdminId();
    }

    /**
     * 获取当前已登录用户 ID，不存在时抛出异常。
     *
     * @return 用户 ID
     */
    public Long requireCurrentUserId() {
        Long userId = getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ResultCode.ACCESS_DENIED, "当前 AI 工具需要登录后使用");
        }
        return userId;
    }

    /**
     * 获取当前已登录管理员 ID，不存在时抛出异常。
     *
     * @return 管理员 ID
     */
    public Long requireCurrentAdminId() {
        Long adminId = getCurrentAdminId();
        if (adminId == null) {
            throw new BusinessException(ResultCode.ACCESS_DENIED, "当前 AI 工具需要管理端登录后使用");
        }
        return adminId;
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
        requireContext().getToolTraces().add(new AiToolCallItem(toolName, toolCategory, success, summary));
    }

    /**
     * 获取当前请求期间的工具调用摘要。
     *
     * @return 工具摘要列表
     */
    public List<AiToolCallItem> getToolTraces() {
        return List.copyOf(requireContext().getToolTraces());
    }

    /**
     * 创建新的请求上下文对象，供一次完整的模型调用复用。
     *
     * @param userId 普通用户 ID
     * @param adminId 管理员 ID
     * @return 请求上下文
     */
    public AiToolRequestContext createContext(Long userId, Long adminId) {
        return new AiToolRequestContext(userId, adminId, new CopyOnWriteArrayList<>());
    }

    /**
     * 获取当前请求上下文。
     *
     * @return 请求上下文
     */
    private AiToolRequestContext requireContext() {
        AiToolRequestContext context = CURRENT_CONTEXT.get();
        if (context == null) {
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "AI 工具上下文不存在，请稍后重试");
        }
        return context;
    }

    /**
     * 单次 AI 调用绑定的工具上下文。
     */
    public static final class AiToolRequestContext {

        private final Long userId;
        private final Long adminId;
        private final List<AiToolCallItem> toolTraces;

        public AiToolRequestContext(Long userId, Long adminId, List<AiToolCallItem> toolTraces) {
            this.userId = userId;
            this.adminId = adminId;
            this.toolTraces = toolTraces;
        }

        public Long getUserId() {
            return userId;
        }

        public Long getAdminId() {
            return adminId;
        }

        public List<AiToolCallItem> getToolTraces() {
            return toolTraces;
        }
    }
}

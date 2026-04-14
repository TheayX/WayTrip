package com.travel.service.ai.tool;

import com.travel.common.exception.BusinessException;
import com.travel.common.result.ResultCode;
import org.springframework.stereotype.Component;

/**
 * AI 工具上下文持有器，统一保存当前请求的用户身份。
 */
@Component
public class AiToolContextHolder {

    private static final ThreadLocal<Long> CURRENT_USER_ID = new ThreadLocal<>();

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
     * 清理当前请求上下文。
     */
    public void clear() {
        CURRENT_USER_ID.remove();
    }
}

package com.travel.service.ai.memory;

import com.travel.common.exception.BusinessException;
import com.travel.common.result.ResultCode;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.UUID;

/**
 * AI 会话 ID 服务，统一生成和校验会话标识。
 */
@Service
public class AiSessionIdService {

    /**
     * 创建新的 AI 会话 ID。
     *
     * @return 会话 ID
     */
    public String createSessionId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 校验并标准化已有会话 ID。
     *
     * @param sessionId 会话 ID
     * @return 标准化后的会话 ID
     */
    public String normalizeSessionId(String sessionId) {
        String safe = sessionId == null ? "" : sessionId.trim();
        if (!StringUtils.hasText(safe)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "会话ID不能为空");
        }
        if (safe.length() > 64) {
            safe = safe.substring(0, 64);
        }
        safe = safe.replaceAll("[^A-Za-z0-9_-]", "");
        if (!StringUtils.hasText(safe)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "会话ID不合法");
        }
        return safe;
    }
}

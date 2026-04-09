package com.travel.util.mask;

import org.springframework.util.StringUtils;

/**
 * 脱敏工具类。
 * <p>
 * 提供手机号等敏感信息的统一脱敏能力。
 */
public final class MaskUtils {

    /**
     * 工具类不允许实例化。
     */
    private MaskUtils() {
    }

    /**
     * 对手机号进行脱敏处理。
     *
     * @param phone 原始手机号
     * @return 脱敏后的手机号；若为空或格式不合法则返回原值
     */
    public static String maskPhone(String phone) {
        if (!StringUtils.hasText(phone)) {
            return phone;
        }
        String normalized = phone.trim();
        // 非标准手机号直接返回原值，避免把邮箱或其他标识误处理成异常格式。
        if (!normalized.matches("^1\\d{10}$")) {
            return normalized;
        }
        return normalized.substring(0, 3) + "****" + normalized.substring(7);
    }
}


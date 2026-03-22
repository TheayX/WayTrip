package com.travel.util;

import org.springframework.util.StringUtils;

/**
 * 脱敏工具
 */
public final class MaskUtil {

    private MaskUtil() {
    }

    public static String maskPhone(String phone) {
        if (!StringUtils.hasText(phone)) {
            return phone;
        }
        String normalized = phone.trim();
        if (!normalized.matches("^1\\d{10}$")) {
            return normalized;
        }
        return normalized.substring(0, 3) + "****" + normalized.substring(7);
    }
}


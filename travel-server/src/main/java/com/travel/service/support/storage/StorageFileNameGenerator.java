package com.travel.service.support.storage;

import cn.hutool.extra.pinyin.PinyinUtil;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 存储文件名生成器，统一生成上传落盘文件名。
 */
@Component
public class StorageFileNameGenerator {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    public String generate(String prefix, String tag, String extension) {
        String timestamp = LocalDateTime.now().format(DATE_TIME_FORMATTER);
        String shortId = UUID.randomUUID().toString().replace("-", "").substring(0, 6);

        StringBuilder builder = new StringBuilder(prefix);
        if (tag != null && !tag.isBlank()) {
            String sanitizedTag = convertToPinyinTag(tag);
            if (!sanitizedTag.isEmpty()) {
                builder.append("_").append(sanitizedTag);
            }
        }

        builder.append("_").append(timestamp);
        builder.append("_").append(shortId);
        builder.append(extension);
        return builder.toString();
    }

    private String convertToPinyinTag(String tag) {
        String pinyin = PinyinUtil.getPinyin(tag, "");
        String sanitized = pinyin.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
        if (sanitized.length() > 20) {
            sanitized = sanitized.substring(0, 20);
        }
        return sanitized;
    }
}

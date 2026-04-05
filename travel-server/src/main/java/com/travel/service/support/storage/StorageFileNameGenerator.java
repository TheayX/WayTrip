package com.travel.service.support.storage;

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

    public String generate(String prefix, String extension) {
        String timestamp = LocalDateTime.now().format(DATE_TIME_FORMATTER);
        String shortId = UUID.randomUUID().toString().replace("-", "").substring(0, 6);

        StringBuilder builder = new StringBuilder(prefix);
        builder.append("_").append(timestamp);
        builder.append("_").append(shortId);
        builder.append(extension);
        return builder.toString();
    }
}

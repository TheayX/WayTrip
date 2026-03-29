package com.travel.service;

import cn.hutool.extra.pinyin.PinyinUtil;
import com.travel.common.result.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 文件上传公共服务。
 * <p>
 * 统一处理图片和图标上传、文件名生成以及访问地址返回逻辑。
 * 文件名格式：{@code {类型前缀}_{标签}_{日期时间}_{短随机码}.{扩展名}}。
 */
@Slf4j
@Service
public class FileUploadService {

    @Value("${upload.path:./uploads}")
    private String uploadPath;

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    /**
     * 上传图片文件。
     *
     * @param file 上传的文件
     * @param maxSizeMB 最大文件大小（MB）
     * @param logPrefix 日志前缀
     * @param tag 文件标签，可为 {@code null}
     * @return 上传结果
     */
    public ApiResponse<Map<String, String>> uploadImage(MultipartFile file, int maxSizeMB, String logPrefix, String tag) {
        if (file.isEmpty()) {
            return ApiResponse.error(60001, "请选择要上传的文件");
        }

        // 检查文件类型
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return ApiResponse.error(60001, "只能上传图片文件");
        }

        // 检查文件大小
        if (file.getSize() > (long) maxSizeMB * 1024 * 1024) {
            return ApiResponse.error(60001, "图片大小不能超过" + maxSizeMB + "MB");
        }

        try {
            // 生成统一格式文件名
            String extension = getExtension(file.getOriginalFilename());
            String newFilename = generateFilename("img", tag, extension);

            // 创建上传目录
            File uploadDir = new File(uploadPath, "images");
            if (!uploadDir.exists()) {
                uploadDir.mkdirs();
            }

            // 保存文件
            File destFile = new File(uploadDir, newFilename);
            file.transferTo(java.util.Objects.requireNonNull(destFile.getAbsoluteFile()));

            // 返回访问URL
            String url = "/uploads/images/" + newFilename;

            Map<String, String> result = new HashMap<>();
            result.put("url", url);
            result.put("filename", newFilename);

            log.info("{}上传成功: {}, 保存路径: {}", logPrefix, url, destFile.getAbsolutePath());
            return ApiResponse.success(result);

        } catch (IOException e) {
            log.error("{}上传失败", logPrefix, e);
            return ApiResponse.error(60002, logPrefix + "上传失败");
        }
    }

    /**
     * 上传图片文件（无标签，保持向后兼容）。
     *
     * @param file 上传的文件
     * @param maxSizeMB 最大文件大小（MB）
     * @param logPrefix 日志前缀
     * @return 上传结果
     */
    public ApiResponse<Map<String, String>> uploadImage(MultipartFile file, int maxSizeMB, String logPrefix) {
        return uploadImage(file, maxSizeMB, logPrefix, null);
    }

    /**
     * 上传图标文件。
     *
     * @param file 上传的文件
     * @param maxSizeMB 最大文件大小（MB）
     * @param logPrefix 日志前缀
     * @param tag 文件标签，可为 {@code null}
     * @return 上传结果
     */
    public ApiResponse<Map<String, String>> uploadIcon(MultipartFile file, int maxSizeMB, String logPrefix, String tag) {
        if (file.isEmpty()) {
            return ApiResponse.error(60001, "请选择要上传的文件");
        }

        // 检查文件类型
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return ApiResponse.error(60001, "只能上传图片文件");
        }

        // 检查文件大小
        if (file.getSize() > (long) maxSizeMB * 1024 * 1024) {
            return ApiResponse.error(60001, "图片大小不能超过" + maxSizeMB + "MB");
        }

        try {
            // 生成统一格式文件名
            String extension = getExtension(file.getOriginalFilename());
            String newFilename = generateFilename("icon", tag, extension);

            // 创建上传目录 (使用 icons)
            File uploadDir = new File(uploadPath, "icons");
            if (!uploadDir.exists()) {
                uploadDir.mkdirs();
            }

            // 保存文件
            File destFile = new File(uploadDir, newFilename);
            file.transferTo(java.util.Objects.requireNonNull(destFile.getAbsoluteFile()));

            // 返回访问URL
            String url = "/uploads/icons/" + newFilename;

            Map<String, String> result = new HashMap<>();
            result.put("url", url);
            result.put("filename", newFilename);

            log.info("{}上传成功: {}, 保存路径: {}", logPrefix, url, destFile.getAbsolutePath());
            return ApiResponse.success(result);

        } catch (IOException e) {
            log.error("{}上传失败", logPrefix, e);
            return ApiResponse.error(60002, logPrefix + "上传失败");
        }
    }

    /**
     * 上传图标文件（无标签，保持向后兼容）。
     *
     * @param file 上传的文件
     * @param maxSizeMB 最大文件大小（MB）
     * @param logPrefix 日志前缀
     * @return 上传结果
     */
    public ApiResponse<Map<String, String>> uploadIcon(MultipartFile file, int maxSizeMB, String logPrefix) {
        return uploadIcon(file, maxSizeMB, logPrefix, null);
    }

    // ==================== 私有工具方法 ====================

    /**
     * 生成统一格式的文件名
     * 格式：{prefix}_{tag}_{yyyyMMdd_HHmmss}_{6位随机码}.{ext}
     * 示例：img_xihu_20260308_232057_a3b5c7.jpg
     *
     * @param prefix    类型前缀（如 img、icon）
     * @param tag       标签（中文会转拼音），可为 null
     * @param extension 文件扩展名（含点号）
     */
    private String generateFilename(String prefix, String tag, String extension) {
        String timestamp = LocalDateTime.now().format(DATE_TIME_FORMATTER);
        String shortId = UUID.randomUUID().toString().replace("-", "").substring(0, 6);

        StringBuilder sb = new StringBuilder();
        sb.append(prefix);

        // 处理标签：中文转拼音，英文保留，去除特殊字符
        if (tag != null && !tag.isBlank()) {
            String sanitizedTag = convertToPinyinTag(tag);
            if (!sanitizedTag.isEmpty()) {
                sb.append("_").append(sanitizedTag);
            }
        }

        sb.append("_").append(timestamp);
        sb.append("_").append(shortId);
        sb.append(extension);

        return sb.toString();
    }

    /**
     * 将标签转换为拼音格式（适用于文件名）
     * - 中文字符转拼音（无声调、小写）
     * - 英文字符保留
     * - 去除其他特殊字符
     * - 最大长度限制为 20 字符
     *
     * 示例：
     * "西湖" → "xihu"
     * "黄山风景区" → "huangshanfengjingqu"
     * "Hello世界" → "helloshijie"
     */
    private String convertToPinyinTag(String tag) {
        // 使用 Hutool 的 PinyinUtil 将中文转为拼音（无分隔符）
        String pinyin = PinyinUtil.getPinyin(tag, "");
        // 只保留字母和数字，转小写
        String sanitized = pinyin.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
        // 限制最大长度
        if (sanitized.length() > 20) {
            sanitized = sanitized.substring(0, 20);
        }
        return sanitized;
    }

    /**
     * 从文件名中提取扩展名（含点号）
     */
    private String getExtension(String originalFilename) {
        if (originalFilename != null && originalFilename.contains(".")) {
            return originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        return "";
    }
}

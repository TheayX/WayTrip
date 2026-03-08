package com.travel.service;

import com.travel.common.result.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 文件上传公共服务
 */
@Slf4j
@Service
public class FileUploadService {

    @Value("${upload.path:./uploads}")
    private String uploadPath;

    /**
     * 上传图片文件
     *
     * @param file       上传的文件
     * @param maxSizeMB  最大文件大小（MB）
     * @param logPrefix  日志前缀（如 "文件" / "头像"）
     * @return 上传结果
     */
    public ApiResponse<Map<String, String>> uploadImage(MultipartFile file, int maxSizeMB, String logPrefix) {
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
            // 生成文件名
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String newFilename = UUID.randomUUID().toString().replace("-", "") + extension;

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
}


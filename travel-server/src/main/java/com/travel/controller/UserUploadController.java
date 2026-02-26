package com.travel.controller;

import com.travel.common.result.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 用户端文件上传接口（头像等）
 */
@Slf4j
@Tag(name = "用户端文件上传")
@RestController
@RequestMapping("/api/v1/upload")
public class UserUploadController {

    @Value("${upload.path:./uploads}")
    private String uploadPath;

    @Operation(summary = "上传头像")
    @PostMapping("/avatar")
    public ApiResponse<Map<String, String>> uploadAvatar(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ApiResponse.error(60001, "请选择要上传的文件");
        }

        // 检查文件类型
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return ApiResponse.error(60001, "只能上传图片文件");
        }

        // 检查文件大小 (2MB)
        if (file.getSize() > 2 * 1024 * 1024) {
            return ApiResponse.error(60001, "头像图片大小不能超过2MB");
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
            file.transferTo(destFile.getAbsoluteFile());

            // 返回访问URL
            String url = "/uploads/images/" + newFilename;

            Map<String, String> result = new HashMap<>();
            result.put("url", url);
            result.put("filename", newFilename);

            log.info("头像上传成功: {}, 保存路径: {}", url, destFile.getAbsolutePath());
            return ApiResponse.success(result);

        } catch (IOException e) {
            log.error("头像上传失败", e);
            return ApiResponse.error(60002, "头像上传失败");
        }
    }
}


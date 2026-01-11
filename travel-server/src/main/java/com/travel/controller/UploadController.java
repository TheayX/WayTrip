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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 文件上传接口
 */
@Slf4j
@Tag(name = "文件上传")
@RestController
@RequestMapping("/api/admin/v1/upload")
public class UploadController {

    @Value("${upload.path:./uploads}")
    private String uploadPath;

    @Value("${server.port:8080}")
    private String serverPort;

    @Operation(summary = "上传图片")
    @PostMapping("/image")
    public ApiResponse<Map<String, String>> uploadImage(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ApiResponse.error(60001, "请选择要上传的文件");
        }

        // 检查文件类型
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return ApiResponse.error(60001, "只能上传图片文件");
        }

        // 检查文件大小 (5MB)
        if (file.getSize() > 5 * 1024 * 1024) {
            return ApiResponse.error(60001, "图片大小不能超过5MB");
        }

        try {
            // 生成文件名
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String newFilename = UUID.randomUUID().toString().replace("-", "") + extension;

            // 使用绝对路径创建上传目录
            File uploadDir = new File(uploadPath, "images");
            if (!uploadDir.exists()) {
                uploadDir.mkdirs();
            }

            // 保存文件（使用绝对路径）
            File destFile = new File(uploadDir, newFilename);
            file.transferTo(destFile.getAbsoluteFile());

            // 返回访问URL
            String url = "/uploads/images/" + newFilename;
            
            Map<String, String> result = new HashMap<>();
            result.put("url", url);
            result.put("filename", newFilename);

            log.info("文件上传成功: {}, 保存路径: {}", url, destFile.getAbsolutePath());
            return ApiResponse.success(result);

        } catch (IOException e) {
            log.error("文件上传失败", e);
            return ApiResponse.error(60002, "文件上传失败");
        }
    }
}

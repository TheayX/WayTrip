package com.travel.controller.app;

import com.travel.common.result.ApiResponse;
import com.travel.service.FileUploadService;
import com.travel.util.web.UserContextHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 用户端文件上传控制器，负责头像等资源上传接口。
 * <p>
 * 用户侧上传能力目前只开放头像，避免普通用户直接写入景点或攻略素材目录。
 */
@Tag(name = "用户端-文件上传", description = "用户端头像等文件上传接口")
@RestController
@RequestMapping("/api/v1/upload")
@RequiredArgsConstructor
public class UploadController {

    private final FileUploadService fileUploadService;

    @Operation(summary = "上传头像")
    @PostMapping("/avatar")
    public ApiResponse<Map<String, String>> uploadAvatar(@RequestParam("file") MultipartFile file) {
        // 头像文件归属当前登录用户，上传服务会基于 userId 生成稳定路径。
        return fileUploadService.uploadAvatar(file, UserContextHolder.getUserId());
    }
}

package com.travel.controller.app;

import com.travel.common.result.ApiResponse;
import com.travel.service.FileUploadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 用户端文件上传接口（头像等）
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
        return fileUploadService.uploadImage(file, 2, "头像");
    }
}

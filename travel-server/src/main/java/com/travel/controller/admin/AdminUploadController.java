package com.travel.controller.admin;

import com.travel.common.result.ApiResponse;
import com.travel.service.FileUploadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 管理端文件上传接口
 */
@Tag(name = "管理端-文件上传", description = "管理端文件上传相关接口")
@RestController
@RequestMapping("/api/admin/v1/upload")
@RequiredArgsConstructor
public class AdminUploadController {

    private final FileUploadService fileUploadService;

    @Operation(summary = "上传图片")
    @PostMapping("/image")
    public ApiResponse<Map<String, String>> uploadImage(@RequestParam("file") MultipartFile file) {
        return fileUploadService.uploadImage(file, 5, "文件");
    }
}

package com.travel.controller.admin;

import com.travel.common.result.ApiResponse;
import com.travel.service.FileUploadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 管理端文件上传控制器，负责后台资源上传接口。
 */
@Tag(name = "管理端-文件上传", description = "管理端文件上传相关接口")
@RestController
@RequestMapping("/api/admin/v1/upload")
@RequiredArgsConstructor
public class AdminUploadController {

    private final FileUploadService fileUploadService;

    @Operation(summary = "上传图片", description = "上传图片文件，可传入 tag 参数用于文件命名（如景点名，中文会自动转拼音）")
    @PostMapping("/image")
    public ApiResponse<Map<String, String>> uploadImage(
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "文件标签（如景点名'西湖'，会转为拼音'xihu'用于文件命名）")
            @RequestParam(value = "tag", required = false) String tag) {
        return fileUploadService.uploadImage(file, 5, "文件", tag);
    }

    @Operation(summary = "上传图标", description = "上传图标文件，可传入 tag 参数用于文件命名")
    @PostMapping("/icon")
    public ApiResponse<Map<String, String>> uploadIcon(
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "文件标签（如分类名，会转为拼音用于文件命名）")
            @RequestParam(value = "tag", required = false) String tag) {
        return fileUploadService.uploadIcon(file, 2, "图标", tag);
    }
}

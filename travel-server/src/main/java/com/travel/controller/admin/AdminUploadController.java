package com.travel.controller.admin;

import com.travel.common.result.ApiResponse;
import com.travel.service.FileUploadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
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

    @Operation(summary = "上传图片", description = "按资源场景上传图片，例如景点封面、景点相册、攻略封面与首页轮播图")
    @PostMapping("/image")
    public ApiResponse<Map<String, String>> uploadImage(
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "上传场景：spot、guide、banner")
            @RequestParam("scene") String scene,
            @Parameter(description = "资源类型：景点场景下可传 cover 或 gallery")
            @RequestParam(value = "assetType", required = false) String assetType,
            @Parameter(description = "业务名称，用于生成景点或攻略 slug")
            @RequestParam(value = "name", required = false) String name) {
        return switch (scene) {
            case "spot" -> resolveSpotUpload(file, assetType, name);
            case "guide" -> fileUploadService.uploadGuideCover(file, name);
            case "banner" -> fileUploadService.uploadBanner(file);
            default -> ApiResponse.error(60001, "不支持的图片上传场景");
        };
    }

    @Operation(summary = "上传图标", description = "上传分类图标，文件会存入统一图标目录")
    @PostMapping("/icon")
    public ApiResponse<Map<String, String>> uploadIcon(
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "分类名称，用于生成更清晰的图标文件名前缀")
            @RequestParam(value = "name", required = false) String name) {
        return fileUploadService.uploadCategoryIcon(file, name);
    }

    /**
     * 景点上传必须显式区分封面与相册，避免后台误把素材存到错误目录。
     */
    private ApiResponse<Map<String, String>> resolveSpotUpload(MultipartFile file, String assetType, String name) {
        if (!StringUtils.hasText(assetType)) {
            return ApiResponse.error(60001, "景点图片必须指定 assetType");
        }
        return switch (assetType) {
            case "cover" -> fileUploadService.uploadSpotCover(file, name);
            case "gallery" -> fileUploadService.uploadSpotGallery(file, name);
            default -> ApiResponse.error(60001, "不支持的景点图片类型");
        };
    }
}

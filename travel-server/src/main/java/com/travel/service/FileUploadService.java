package com.travel.service;

import com.travel.common.result.ApiResponse;
import com.travel.service.support.storage.ImageUploadValidator;
import com.travel.service.support.storage.LocalFileStorageService;
import com.travel.service.support.storage.StoredFileInfo;
import com.travel.service.support.storage.UploadPathResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 文件上传公共服务。
 * <p>
 * 对 controller 继续暴露统一上传入口，内部把校验、命名和落盘拆给独立组件处理。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileUploadService {

    private final ImageUploadValidator imageUploadValidator;
    private final LocalFileStorageService localFileStorageService;
    private final UploadPathResolver uploadPathResolver;

    /**
     * 上传景点封面图。
     */
    public ApiResponse<Map<String, String>> uploadSpotCover(MultipartFile file, String spotName) {
        return uploadFile(file, 5, "景点封面", uploadPathResolver.resolveSpotCover(spotName));
    }

    /**
     * 上传景点相册图。
     */
    public ApiResponse<Map<String, String>> uploadSpotGallery(MultipartFile file, String spotName) {
        return uploadFile(file, 5, "景点相册", uploadPathResolver.resolveSpotGallery(spotName));
    }

    /**
     * 上传攻略封面图。
     */
    public ApiResponse<Map<String, String>> uploadGuideCover(MultipartFile file, String guideTitle) {
        return uploadFile(file, 5, "攻略封面", uploadPathResolver.resolveGuideCover(guideTitle));
    }

    /**
     * 上传轮播图。
     */
    public ApiResponse<Map<String, String>> uploadBanner(MultipartFile file) {
        return uploadFile(file, 5, "轮播图", uploadPathResolver.resolveBanner());
    }

    /**
     * 上传分类图标。
     */
    public ApiResponse<Map<String, String>> uploadCategoryIcon(MultipartFile file, String categoryName) {
        return uploadFile(file, 2, "分类图标", uploadPathResolver.resolveCategoryIcon(categoryName));
    }

    /**
     * 上传用户头像。
     */
    public ApiResponse<Map<String, String>> uploadAvatar(MultipartFile file, Long userId) {
        return uploadFile(file, 2, "头像", uploadPathResolver.resolveAvatar(userId));
    }

    /**
     * 收口公共上传编排，避免不同业务场景各自重复校验与落盘逻辑。
     */
    private ApiResponse<Map<String, String>> uploadFile(MultipartFile file, int maxSizeMB, String logPrefix, UploadPathResolver.UploadTarget uploadTarget) {
        String validationMessage = imageUploadValidator.validateImage(file, maxSizeMB);
        if (validationMessage != null) {
            return ApiResponse.error(60001, validationMessage);
        }

        try {
            StoredFileInfo storedFile = localFileStorageService.store(file, uploadTarget.directory(), uploadTarget.filenamePrefix());
            return successResponse(logPrefix, storedFile);
        } catch (IOException e) {
            log.error("{}上传失败", logPrefix, e);
            return ApiResponse.error(60002, logPrefix + "上传失败");
        }
    }

    private ApiResponse<Map<String, String>> successResponse(String logPrefix, StoredFileInfo storedFile) {
        Map<String, String> result = new HashMap<>();
        result.put("url", storedFile.getUrl());
        result.put("filename", storedFile.getFilename());
        log.info("{}上传成功: {}, 保存路径: {}", logPrefix, storedFile.getUrl(), storedFile.getAbsolutePath());
        return ApiResponse.success(result);
    }
}

package com.travel.service;

import com.travel.common.result.ApiResponse;
import com.travel.service.support.storage.FileValidator;
import com.travel.service.support.storage.LocalFileStorageService;
import com.travel.service.support.storage.StoredFile;
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

    private final FileValidator fileValidator;
    private final LocalFileStorageService localFileStorageService;

    /**
     * 图片上传入口，统一复用图片校验和本地落盘流程。
     */
    public ApiResponse<Map<String, String>> uploadImage(MultipartFile file, int maxSizeMB, String logPrefix, String tag) {
        return uploadFile(file, maxSizeMB, logPrefix, tag, "images", "img");
    }

    public ApiResponse<Map<String, String>> uploadImage(MultipartFile file, int maxSizeMB, String logPrefix) {
        return uploadImage(file, maxSizeMB, logPrefix, null);
    }

    /**
     * 图标上传入口，统一复用图片校验和本地落盘流程。
     */
    public ApiResponse<Map<String, String>> uploadIcon(MultipartFile file, int maxSizeMB, String logPrefix, String tag) {
        return uploadFile(file, maxSizeMB, logPrefix, tag, "icons", "icon");
    }

    public ApiResponse<Map<String, String>> uploadIcon(MultipartFile file, int maxSizeMB, String logPrefix) {
        return uploadIcon(file, maxSizeMB, logPrefix, null);
    }

    /**
     * 收口公共上传编排，避免图片和图标上传流程继续重复分叉。
     */
    private ApiResponse<Map<String, String>> uploadFile(
        MultipartFile file,
        int maxSizeMB,
        String logPrefix,
        String tag,
        String directory,
        String filenamePrefix
    ) {
        String validationMessage = fileValidator.validateImage(file, maxSizeMB);
        if (validationMessage != null) {
            return ApiResponse.error(60001, validationMessage);
        }

        try {
            StoredFile storedFile = localFileStorageService.store(file, directory, filenamePrefix, tag);
            return successResponse(logPrefix, storedFile);
        } catch (IOException e) {
            log.error("{}上传失败", logPrefix, e);
            return ApiResponse.error(60002, logPrefix + "上传失败");
        }
    }

    private ApiResponse<Map<String, String>> successResponse(String logPrefix, StoredFile storedFile) {
        Map<String, String> result = new HashMap<>();
        result.put("url", storedFile.getUrl());
        result.put("filename", storedFile.getFilename());
        log.info("{}上传成功: {}, 保存路径: {}", logPrefix, storedFile.getUrl(), storedFile.getAbsolutePath());
        return ApiResponse.success(result);
    }
}

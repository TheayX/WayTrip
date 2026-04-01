package com.travel.service.support.storage;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * 图片上传校验器，集中处理图片上传的通用校验逻辑。
 */
@Component
public class ImageUploadValidator {

    public String validateImage(MultipartFile file, int maxSizeMB) {
        if (file.isEmpty()) {
            return "请选择要上传的文件";
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return "只能上传图片文件";
        }

        if (file.getSize() > (long) maxSizeMB * 1024 * 1024) {
            return "图片大小不能超过" + maxSizeMB + "MB";
        }
        return null;
    }

    public String getExtension(String originalFilename) {
        if (originalFilename != null && originalFilename.contains(".")) {
            return originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        return "";
    }
}

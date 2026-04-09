package com.travel.service.support.storage;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * 图片上传校验器，集中处理图片上传的通用校验逻辑。
 * <p>
 * 上传前的基础校验统一放在这里，避免头像、景点图和轮播图各自维护重复规则。
 */
@Component
public class ImageUploadValidator {

    /**
     * 校验图片文件是否符合基本要求。
     *
     * @param file 上传文件
     * @param maxSizeMB 最大体积限制
     * @return 校验失败时返回错误文案，成功时返回 null
     */
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

    /**
     * 提取文件扩展名。
     *
     * @param originalFilename 原始文件名
     * @return 扩展名
     */
    public String getExtension(String originalFilename) {
        if (originalFilename != null && originalFilename.contains(".")) {
            return originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        return "";
    }
}

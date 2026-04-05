package com.travel.service.support.storage;

import cn.hutool.extra.pinyin.PinyinUtil;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 上传路径解析器，统一根据业务场景生成目录和文件名前缀。
 */
@Component
public class UploadPathResolver {

    /**
     * 解析景点封面上传目标。
     */
    public UploadTarget resolveSpotCover(String spotName) {
        String spotSlug = buildSlug(spotName, "spot");
        return new UploadTarget("spot/" + spotSlug + "/cover", "cover_" + spotSlug);
    }

    /**
     * 解析景点相册上传目标。
     */
    public UploadTarget resolveSpotGallery(String spotName) {
        String spotSlug = buildSlug(spotName, "spot");
        return new UploadTarget("spot/" + spotSlug + "/gallery", "gallery_" + spotSlug);
    }

    /**
     * 解析攻略封面上传目标。
     */
    public UploadTarget resolveGuideCover(String guideTitle) {
        String guideSlug = buildSlug(guideTitle, "guide");
        return new UploadTarget("guide", guideSlug);
    }

    /**
     * 解析首页轮播图上传目标。
     */
    public UploadTarget resolveBanner() {
        return new UploadTarget("banner", "banner");
    }

    /**
     * 解析分类图标上传目标。
     */
    public UploadTarget resolveCategoryIcon(String categoryName) {
        String iconName = buildSlug(categoryName, "category");
        return new UploadTarget("icons", iconName);
    }

    /**
     * 解析用户头像上传目标。
     */
    public UploadTarget resolveAvatar(Long userId) {
        String prefix = userId == null ? "avatar" : "user_" + userId;
        return new UploadTarget("avatar", prefix);
    }

    /**
     * 将中文名称收敛为稳定、简洁的拼音 slug，便于目录与文件命名。
     */
    private String buildSlug(String source, String fallback) {
        if (!StringUtils.hasText(source)) {
            return fallback;
        }

        String pinyin = PinyinUtil.getPinyin(source.trim(), "");
        String sanitized = pinyin.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
        if (!StringUtils.hasText(sanitized)) {
            return fallback;
        }
        if (sanitized.length() > 32) {
            return sanitized.substring(0, 32);
        }
        return sanitized;
    }

    /**
     * 上传目标值对象，避免目录和文件名前缀在调用链中分散拼接。
     */
    public record UploadTarget(String directory, String filenamePrefix) {
    }
}

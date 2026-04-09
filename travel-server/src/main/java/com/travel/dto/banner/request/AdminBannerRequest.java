package com.travel.dto.banner.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 管理端轮播图创建或更新请求对象。
 * <p>
 * 后台轮播图新增与编辑共用同一表单结构，保持首页资源维护口径一致。
 */
@Data
public class AdminBannerRequest {

    @NotBlank(message = "图片URL不能为空")
    private String imageUrl;

    private Long spotId;

    private Integer sortOrder = 1;

    private Integer enabled = 1;
}

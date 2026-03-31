package com.travel.dto.banner.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 管理端轮播图创建或更新请求对象。
 */
@Data
public class AdminBannerRequest {

    @NotBlank(message = "图片URL不能为空")
    private String imageUrl;

    private Long spotId;

    private Integer sortOrder = 1;

    private Integer enabled = 1;
}

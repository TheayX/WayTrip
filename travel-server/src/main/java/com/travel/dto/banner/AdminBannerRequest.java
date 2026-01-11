package com.travel.dto.banner;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 管理端轮播图请求
 */
@Data
public class AdminBannerRequest {

    @NotBlank(message = "图片URL不能为空")
    private String imageUrl;

    private Long spotId;

    private Integer sortOrder = 0;

    private Integer enabled = 1;
}

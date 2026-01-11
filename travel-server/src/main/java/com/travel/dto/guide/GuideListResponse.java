package com.travel.dto.guide;

import lombok.Builder;
import lombok.Data;

/**
 * 攻略列表响应
 */
@Data
@Builder
public class GuideListResponse {
    private Long id;
    private String title;
    private String coverImage;
    private String category;
    private String summary;
    private Integer viewCount;
    private String createdAt;
}

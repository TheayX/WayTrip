package com.travel.dto.guide.response;

import lombok.Builder;
import lombok.Data;

/**
 * 用户端攻略列表项响应对象。
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

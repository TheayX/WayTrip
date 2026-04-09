package com.travel.dto.guide.response;

import lombok.Builder;
import lombok.Data;

/**
 * 用户端攻略列表项响应对象。
 * <p>
 * 用于普通攻略列表页，返回卡片展示所需的核心摘要字段。
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

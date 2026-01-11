package com.travel.dto.guide;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 管理端攻略列表响应
 */
@Data
@Builder
public class AdminGuideListResponse {
    private Long id;
    private String title;
    private String coverImage;
    private String category;
    private Integer viewCount;
    private Boolean published;
    private LocalDateTime createdAt;
}

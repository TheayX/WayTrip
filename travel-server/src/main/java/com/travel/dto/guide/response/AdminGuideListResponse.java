package com.travel.dto.guide.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 管理端攻略列表项响应对象。
 * <p>
 * 面向后台表格展示，返回攻略列表所需的轻量字段集合。
 */
@Data
@Builder
public class AdminGuideListResponse {
    private Long id;
    private String title;
    private String coverImage;
    private String category;
    private Long adminId;
    private String adminName;
    private Integer viewCount;
    private Boolean published;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

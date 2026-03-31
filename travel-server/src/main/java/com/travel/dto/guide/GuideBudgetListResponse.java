package com.travel.dto.guide;

import lombok.Builder;
import lombok.Data;

/**
 * 用户端穷游攻略列表项响应对象。
 */
@Data
@Builder
public class GuideBudgetListResponse {
    private Long id;
    private String title;
    private String coverImage;
    private String category;
    private String summary;
    private Integer viewCount;
    private String createdAt;
    private Integer relatedCount;
    private String priceLabel;
}

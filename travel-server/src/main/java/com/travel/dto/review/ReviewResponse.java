package com.travel.dto.review;

import lombok.Builder;
import lombok.Data;

/**
 * 评价记录响应对象。
 */
@Data
@Builder
public class ReviewResponse {
    private Long id;
    private Long userId;
    private Long spotId;
    private String spotName;
    private String coverImageUrl;
    private Integer score;
    private String comment;
    private String nickname;
    private String avatar;
    private String createdAt;
    private String updatedAt;
}


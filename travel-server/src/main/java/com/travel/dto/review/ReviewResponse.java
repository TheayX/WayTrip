package com.travel.dto.review;

import lombok.Builder;
import lombok.Data;

/**
 * 评价响应
 */
@Data
@Builder
public class ReviewResponse {
    private Long id;
    private Long userId;
    private Long spotId;
    private Integer score;
    private String comment;
    private String nickname;
    private String avatar;
    private String createdAt;
}


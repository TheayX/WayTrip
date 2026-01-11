package com.travel.dto.rating;

import lombok.Builder;
import lombok.Data;

/**
 * 评分响应
 */
@Data
@Builder
public class RatingResponse {
    private Long id;
    private Long userId;
    private Long spotId;
    private Integer score;
    private String comment;
    private String nickname;
    private String avatar;
    private String createdAt;
}

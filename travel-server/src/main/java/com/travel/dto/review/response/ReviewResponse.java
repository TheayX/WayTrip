package com.travel.dto.review.response;

import lombok.Builder;
import lombok.Data;

/**
 * 评价记录响应对象。
 * <p>
 * 同时服务景点评论列表、我的评价和后台评价检索等多个展示场景。
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


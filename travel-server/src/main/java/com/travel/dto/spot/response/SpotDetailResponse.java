package com.travel.dto.spot.response;

import lombok.Data;
import lombok.Builder;
import java.math.BigDecimal;
import java.util.List;

/**
 * 用户端景点详情响应对象。
 */
@Data
@Builder
public class SpotDetailResponse {
    private Long id;
    private String name;
    private String coverImage;
    private String description;
    private BigDecimal price;
    private String openTime;
    private String address;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private List<String> images;
    private BigDecimal avgRating;
    private Integer ratingCount;
    private String regionName;
    private String categoryName;
    private Boolean isFavorite;
    private Integer userRating;
    private List<CommentItem> latestComments;
    
    /**
     * 景点详情页最新评论对象。
     */
    @Data
    @Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class CommentItem {
        private Long id;
        private Long userId;
        private String nickname;
        private String avatar;
        private Integer score;
        private String comment;
        private String createdAt;
    }
}

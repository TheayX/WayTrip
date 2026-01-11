package com.travel.dto.guide;

import lombok.Builder;
import lombok.Data;
import java.util.List;

/**
 * 攻略详情响应
 */
@Data
@Builder
public class GuideDetailResponse {
    private Long id;
    private String title;
    private String coverImage;
    private String category;
    private String content;
    private Integer viewCount;
    private String createdAt;
    private List<RelatedSpot> relatedSpots;
    
    @Data
    @Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class RelatedSpot {
        private Long id;
        private String name;
        private String coverImage;
        private String price;
    }
}

package com.travel.dto.guide.response;

import lombok.Builder;
import lombok.Data;
import java.util.List;

/**
 * 用户端攻略详情响应对象。
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
    
    /**
     * 攻略关联景点响应对象。
     */
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

package com.travel.dto.recommendation;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 相似景点邻居预览响应对象。
 */
@Data
public class SimilarityPreviewResponse {

    private Long spotId;
    private String spotName;
    private Integer totalNeighbors;
    private String lastUpdateTime;
    private List<NeighborItem> neighbors;

    /**
     * 相似邻居项对象。
     */
    @Data
    public static class NeighborItem {
        private Long spotId;
        private String spotName;
        private String coverImage;
        private BigDecimal price;
        private BigDecimal avgRating;
        private String categoryName;
        private String regionName;
        private Double similarity;
    }
}

package com.travel.dto.recommendation;

import lombok.Data;

import java.util.List;

@Data
public class SimilarityPreviewResponse {

    private Long spotId;
    private String spotName;
    private Integer totalNeighbors;
    private String lastUpdateTime;
    private List<NeighborItem> neighbors;

    @Data
    public static class NeighborItem {
        private Long spotId;
        private String spotName;
        private String categoryName;
        private String regionName;
        private Double similarity;
    }
}

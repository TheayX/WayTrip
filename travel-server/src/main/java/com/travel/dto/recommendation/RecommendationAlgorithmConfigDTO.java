package com.travel.dto.recommendation;

import lombok.Data;

/**
 * 推荐算法参数配置对象。
 */
@Data
public class RecommendationAlgorithmConfigDTO {

    private Double weightView = 0.5;
    private Double weightFavorite = 1.0;
    private Double weightReviewFactor = 0.4;
    private Double weightOrderPaid = 3.0;
    private Double weightOrderCompleted = 4.0;

    private Double viewSourceFactorHome = 0.9;
    private Double viewSourceFactorSearch = 1.2;
    private Double viewSourceFactorRecommendation = 1.1;
    private Double viewSourceFactorGuide = 1.0;
    private Double viewSourceFactorDetail = 1.0;

    private Integer viewDurationShortThresholdSeconds = 10;
    private Integer viewDurationMediumThresholdSeconds = 60;
    private Integer viewDurationLongThresholdSeconds = 180;
    private Double viewDurationFactorShort = 0.6;
    private Double viewDurationFactorMedium = 1.0;
    private Double viewDurationFactorLong = 1.2;
    private Double viewDurationFactorVeryLong = 1.35;

    private Integer minInteractionsForCF = 3;
    private Integer topKNeighbors = 20;
    private Integer candidateExpandFactor = 2;
    private Integer coldStartExpandFactor = 3;
}

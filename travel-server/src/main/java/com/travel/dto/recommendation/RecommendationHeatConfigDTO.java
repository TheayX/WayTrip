package com.travel.dto.recommendation;

import lombok.Data;

/**
 * 推荐热度策略配置。
 */
@Data
public class RecommendationHeatConfigDTO {

    private Integer heatViewIncrement = 1;
    private Integer heatFavoriteIncrement = 3;
    private Integer heatReviewIncrement = 2;
    private Integer heatOrderPaidIncrement = 5;
    private Integer heatOrderCompletedIncrement = 8;
    private Integer heatViewDedupeWindowMinutes = 30;
    private Double heatRerankFactor = 0.05;
}

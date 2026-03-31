package com.travel.dto.recommendation.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * 推荐热度策略配置参数对象。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RecommendationHeatConfigDTO {

    private Integer heatViewIncrement = 1;
    private Integer heatFavoriteIncrement = 3;
    private Integer heatReviewIncrement = 2;
    private Integer heatOrderPaidIncrement = 5;
    private Integer heatOrderCompletedIncrement = 8;
    private Double heatRerankFactor = 0.05;
}

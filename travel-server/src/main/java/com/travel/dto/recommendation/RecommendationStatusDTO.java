package com.travel.dto.recommendation;

import lombok.Data;

/**
 * 推荐引擎运行状态传输对象。
 */
@Data
public class RecommendationStatusDTO {

    /** 上次矩阵更新时间 */
    private String lastUpdateTime;

    /** 矩阵覆盖用户数 */
    private Integer totalUsers;

    /** 矩阵覆盖景点数 */
    private Integer totalSpots;

    /** 矩阵是否正在计算中 */
    private Boolean computing;
}

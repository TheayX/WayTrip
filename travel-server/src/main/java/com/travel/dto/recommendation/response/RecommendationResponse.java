package com.travel.dto.recommendation.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 推荐结果响应对象，包含推荐列表和可选调试信息。
 */
@Data
public class RecommendationResponse {

    /**
     * 推荐类型：personalized/hot/preference
     */
    private String type;

    /**
     * 推荐列表
     */
    private List<SpotItem> list;

    /**
     * 是否需要设置偏好（冷启动提示）
     */
    private Boolean needPreference;

    /**
     * 管理端调试信息，仅调试预览时返回
     */
    private DebugInfo debugInfo;

    /**
     * 推荐景点项对象。
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SpotItem {
        private Long id;
        private String name;
        private String coverImage;
        private BigDecimal price;
        private BigDecimal avgRating;
        private Integer ratingCount;
        private String categoryName;
        private String regionName;
        private Double score; // 推荐分数
    }

    /**
     * 管理端调试信息对象。
     */
    @Data
    public static class DebugInfo {
        private Long userId;
        private String userNickname;
        private Integer requestLimit;
        private String mode;
        private Boolean refresh;
        private Boolean debugEnabled;
        private String triggerReason;
        private Integer interactionCount;
        private Integer candidateCount;
        private Integer filteredCount;
        private Integer finalCount;
        private List<BehaviorStat> behaviorStats;
        private List<BehaviorDetail> behaviorDetails;
        private List<DebugEntry> userInteractions;
        private List<DebugEntry> candidateScores;
        private List<DebugEntry> filteredScores;
        private List<DebugEntry> rerankedScores;
        private List<DebugEntry> filteredOutItems;
        private List<ResultContribution> resultContributions;
        private List<String> notes;
        private Map<String, Object> extra;
    }

    /**
     * 调试明细项对象。
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DebugEntry {
        private Long spotId;
        private String spotName;
        private Double score;
        private String description;
    }

    /**
     * 推荐结果贡献项对象。
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResultContribution {
        private Long targetSpotId;
        private String targetSpotName;
        private Double finalScore;
        private List<DebugEntry> contributors;
    }

    /**
     * 用户行为统计对象。
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BehaviorStat {
        private String behavior;
        private Integer recordCount;
        private Integer uniqueSpotCount;
        private String description;
    }

    /**
     * 用户行为明细对象。
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BehaviorDetail {
        private String behavior;
        private Long spotId;
        private String spotName;
        private Double score;
        private String description;
    }
}

package com.travel.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 业务缓存配置。
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.cache")
public class AppCacheProperties {

    private Recommendation recommendation = new Recommendation();
    private Spot spot = new Spot();

    @Data
    public static class Recommendation {

        /**
         * 用户推荐结果缓存时长，单位：分钟。
         */
        private Integer userRecTtlMinutes = 60;

        /**
         * 相似度矩阵缓存时长，单位：小时。
         */
        private Integer similarityTtlHours = 24;
    }

    @Data
    public static class Spot {

        /**
         * 景点浏览热度去重窗口，单位：分钟。
         */
        private Integer heatViewDedupeWindowMinutes = 30;
    }
}

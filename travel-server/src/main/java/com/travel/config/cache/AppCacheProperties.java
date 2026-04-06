package com.travel.config.cache;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 业务缓存配置。
 * <p>
 * 用于绑定 {@code app.cache} 前缀下的缓存相关参数。
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.cache")
public class AppCacheProperties {

    /**
     * 推荐模块缓存配置。
     */
    private Recommendation recommendation = new Recommendation();

    /**
     * 首页缓存配置。
     */
    private Home home = new Home();

    @Data
    /**
     * 推荐缓存细分配置。
     */
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
    /**
     * 首页缓存细分配置。
     */
    public static class Home {

        /**
         * 首页热门景点缓存时长，单位：分钟。
         */
        private Integer hotSpotsTtlMinutes = 10;

        /**
         * 首页轮播图缓存时长，单位：分钟。
         */
        private Integer bannersTtlMinutes = 10;
    }

}

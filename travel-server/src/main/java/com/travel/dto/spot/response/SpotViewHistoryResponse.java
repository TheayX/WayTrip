package com.travel.dto.spot.response;

import lombok.Builder;
import lombok.Data;

/**
 * 用户最近浏览景点响应对象。
 * <p>
 * 用于“我的足迹”一类页面展示用户最近浏览过的景点摘要。
 */
@Data
@Builder
public class SpotViewHistoryResponse {
    private Long id;
    private String name;
    private String coverImage;
    private String regionName;
    private String categoryName;
    private String viewedAt;
}

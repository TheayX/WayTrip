package com.travel.dto.home.response;

import com.travel.dto.home.item.RecentViewedSpotItem;
import lombok.Data;

import java.util.List;

/**
 * 最近都在看响应对象。
 * <p>
 * 统一封装最近浏览热点列表和统计天数，便于首页模块直接渲染。
 */
@Data
public class RecentViewedSpotResponse {
    private List<RecentViewedSpotItem> list;
    private Integer days;
}

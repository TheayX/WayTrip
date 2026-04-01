package com.travel.dto.home.response;

import com.travel.dto.home.item.RecentViewedSpotItem;
import lombok.Data;

import java.util.List;

/**
 * 最近都在看响应对象。
 */
@Data
public class RecentViewedSpotResponse {
    private List<RecentViewedSpotItem> list;
    private Integer days;
}

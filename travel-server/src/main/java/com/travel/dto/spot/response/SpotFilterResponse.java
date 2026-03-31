package com.travel.dto.spot.response;

import lombok.Data;
import lombok.Builder;
import java.util.List;

/**
 * 景点筛选项响应对象。
 */
@Data
@Builder
public class SpotFilterResponse {
    private List<FilterItem> regions;
    /**
     * 地区树（parent_id 层级结构）
     */
    private List<FilterItem> regionTree;
    /**
     * 扁平分类列表（兼容旧版客户端）
     */
    private List<FilterItem> categories;
    /**
     * 分类树（parent_id 层级结构）
     */
    private List<FilterItem> categoryTree;
    
    /**
     * 景点筛选项对象。
     */
    @Data
    @Builder
    public static class FilterItem {
        private Long id;
        private String name;
        private Long parentId;
        private String iconUrl;
        private List<FilterItem> children;
    }
}

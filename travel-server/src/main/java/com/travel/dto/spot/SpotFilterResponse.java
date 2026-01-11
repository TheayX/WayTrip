package com.travel.dto.spot;

import lombok.Data;
import lombok.Builder;
import java.util.List;

/**
 * 景点筛选选项响应
 */
@Data
@Builder
public class SpotFilterResponse {
    private List<FilterItem> regions;
    private List<FilterItem> categories;
    
    @Data
    @Builder
    public static class FilterItem {
        private Long id;
        private String name;
    }
}

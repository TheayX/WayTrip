package com.travel.dto.guide.request;

import lombok.Data;

/**
 * 管理端攻略列表筛选参数对象。
 * <p>
 * 用于后台攻略管理页接收分页、关键词、分类和发布态等筛选条件。
 */
@Data
public class AdminGuideListRequest {
    private Integer page = 1;
    private Integer pageSize = 10;
    private String keyword;
    private String category;
    private Integer published;
}

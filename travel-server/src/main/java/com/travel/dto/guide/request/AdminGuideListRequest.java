package com.travel.dto.guide.request;

import lombok.Data;

import java.time.LocalDate;

/**
 * 管理端攻略列表筛选参数对象。
 * <p>
 * 用于后台攻略管理页接收分页、关键词、分类、发布态和时间范围等筛选条件。
 */
@Data
public class AdminGuideListRequest {
    private Integer page = 1;
    private Integer pageSize = 10;
    private String keyword;
    private String category;
    private Integer published;
    private LocalDate createdStartDate;
    private LocalDate createdEndDate;
    private LocalDate updatedStartDate;
    private LocalDate updatedEndDate;
}

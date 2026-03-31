package com.travel.dto.user.request;

import lombok.Data;

import java.time.LocalDate;

/**
 * 管理端浏览行为列表筛选参数。
 */
@Data
public class AdminUserViewListRequest {

    private String nickname;

    private String spotName;

    private String source;

    private LocalDate startDate;

    private LocalDate endDate;

    private Integer page = 1;

    private Integer pageSize = 10;
}

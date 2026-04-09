package com.travel.dto.user.request;

import lombok.Data;

import java.time.LocalDate;

/**
 * 管理端浏览行为列表筛选参数。
 * <p>
 * 承接后台用户浏览行为的昵称、景点和时间范围筛选条件。
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

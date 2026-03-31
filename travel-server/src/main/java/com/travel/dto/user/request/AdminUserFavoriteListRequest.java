package com.travel.dto.user.request;

import lombok.Data;

import java.time.LocalDate;

/**
 * 管理端用户收藏列表筛选参数。
 */
@Data
public class AdminUserFavoriteListRequest {

    private String nickname;

    private String spotName;

    private LocalDate startDate;

    private LocalDate endDate;

    private Integer page = 1;

    private Integer pageSize = 10;
}

package com.travel.dto.review.request;

import lombok.Data;

/**
 * 管理端评价列表筛选参数对象。
 * <p>
 * 用于后台评价检索页接收昵称、景点和时间范围等筛选条件。
 */
@Data
public class AdminReviewListRequest {

    private String nickname;

    private String spotName;

    private Integer page = 1;

    private Integer pageSize = 10;
}

package com.travel.dto.review.request;

import lombok.Data;

/**
 * 用户端口碑流查询参数对象。
 * <p>
 * 面向游客口碑页，承接分页与正负向口碑分栏筛选条件。
 */
@Data
public class ReviewFeedRequest {
    private Integer page = 1;
    private Integer pageSize = 10;
    private String type = "positive";
}

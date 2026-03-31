package com.travel.dto.review;

import lombok.Data;

/**
 * 用户端口碑流查询参数对象。
 */
@Data
public class ReviewFeedRequest {
    private Integer page = 1;
    private Integer pageSize = 10;
    private String type = "positive";
}

package com.travel.dto.review;

import lombok.Data;

@Data
public class AdminReviewListRequest {

    private String nickname;

    private String spotName;

    private Integer page = 1;

    private Integer pageSize = 10;
}

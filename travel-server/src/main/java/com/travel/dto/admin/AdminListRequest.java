package com.travel.dto.admin;

import lombok.Data;

@Data
public class AdminListRequest {

    private Integer page = 1;
    private Integer pageSize = 10;
    private String keyword;
    private Integer status;
}

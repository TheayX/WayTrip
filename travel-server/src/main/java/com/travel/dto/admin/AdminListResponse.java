package com.travel.dto.admin;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class AdminListResponse {

    private List<AdminItem> list;
    private long total;
    private int page;
    private int pageSize;

    @Data
    public static class AdminItem {
        private Long id;
        private String username;
        private String realName;
        private Integer status;
        private LocalDateTime lastLoginAt;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }
}

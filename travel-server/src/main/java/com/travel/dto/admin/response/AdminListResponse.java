package com.travel.dto.admin.response;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 管理员列表响应对象。
 * <p>
 * 统一封装后台管理员分页列表与列表项结构，便于管理端表格直接消费。
 */
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

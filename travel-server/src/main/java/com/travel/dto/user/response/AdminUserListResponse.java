package com.travel.dto.user.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 管理端用户列表响应对象。
 * <p>
 * 统一封装后台用户列表和列表项结构，便于用户管理页直接消费。
 */
@Data
public class AdminUserListResponse {

    private List<UserItem> list;
    private Long total;
    private Integer page;
    private Integer pageSize;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserItem {
        private Long id;
        private String nickname;
        private String avatar;
        private String phone;
        private Integer orderCount;
        private Integer favoriteCount;
        private Integer ratingCount;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }
}

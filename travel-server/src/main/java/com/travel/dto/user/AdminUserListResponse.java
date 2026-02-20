package com.travel.dto.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 管理端用户列表响应
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

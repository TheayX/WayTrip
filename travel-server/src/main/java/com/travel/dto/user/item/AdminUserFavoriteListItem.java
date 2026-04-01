package com.travel.dto.user.item;

import lombok.Builder;
import lombok.Data;

/**
 * 管理端用户收藏列表项。
 */
@Data
@Builder
public class AdminUserFavoriteListItem {

    private Long id;

    private Long userId;

    private String nickname;

    private Long spotId;

    private String spotName;

    private String coverImage;

    private String createdAt;
}

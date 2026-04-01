package com.travel.dto.user.item;

import lombok.Builder;
import lombok.Data;

/**
 * 管理端浏览行为列表项。
 */
@Data
@Builder
public class AdminUserViewListItem {

    private Long id;

    private Long userId;

    private String nickname;

    private Long spotId;

    private String spotName;

    private String coverImage;

    private String source;

    private Integer duration;

    private String createdAt;
}

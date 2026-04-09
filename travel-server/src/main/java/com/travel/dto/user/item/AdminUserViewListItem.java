package com.travel.dto.user.item;

import lombok.Builder;
import lombok.Data;

/**
 * 管理端浏览行为列表项。
 * <p>
 * 面向后台运营洞察页，承接单条浏览行为记录的展示字段。
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

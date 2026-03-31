package com.travel.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户景点浏览记录持久化实体，对应 user_spot_view 表。
 */
@Data
@TableName("user_spot_view")
public class UserSpotView {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long spotId;

    /**
     * 浏览来源：home/search/recommendation/guide/detail 等原始页面来源
     */
    private String viewSource;

    /**
     * 停留时长（秒）
     */
    private Integer viewDuration;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}

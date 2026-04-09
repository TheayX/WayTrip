package com.travel.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户持久化实体，对应 user 表。
 * <p>
 * 同时承接 Web 注册用户和小程序 openid 用户，因此账号字段设计兼容双端登录模式。
 */
@Data
@TableName("user")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String openid;

    private String nickname;

    private String phone;

    private String password;

    private String avatarUrl;

    @TableField("last_login_at")
    private LocalDateTime lastLoginAt;

    @TableField("is_deleted")
    private Integer isDeleted;

    /**
     * 用户偏好标签（非数据库字段，通过关联查询获取）
     */
    @TableField(exist = false)
    private String preferences;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}

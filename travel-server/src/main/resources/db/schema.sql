-- 旅游推荐系统数据库初始化脚本
-- 创建数据库
CREATE DATABASE IF NOT EXISTS waytrip_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE waytrip_db;

-- 1. 用户表
CREATE TABLE IF NOT EXISTS `user` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `openid` VARCHAR(64) NOT NULL COMMENT '微信OpenID',
    `nickname` VARCHAR(64) DEFAULT '' COMMENT '用户昵称',
    `avatar` VARCHAR(512) DEFAULT '' COMMENT '头像URL',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_openid` (`openid`),
    KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 2. 用户偏好标签表
CREATE TABLE IF NOT EXISTS `user_preference` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    `tag` VARCHAR(32) NOT NULL COMMENT '偏好标签',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_tag` (`user_id`, `tag`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户偏好标签表';

-- 3. 管理员表
CREATE TABLE IF NOT EXISTS `admin` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '管理员ID',
    `username` VARCHAR(32) NOT NULL COMMENT '用户名',
    `password` VARCHAR(128) NOT NULL COMMENT '密码（BCrypt加密）',
    `real_name` VARCHAR(32) DEFAULT '' COMMENT '真实姓名',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    `last_login_at` DATETIME DEFAULT NULL COMMENT '最后登录时间',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='管理员表';

-- 4. 地区表
CREATE TABLE IF NOT EXISTS `region` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '地区ID',
    `name` VARCHAR(32) NOT NULL COMMENT '地区名称',
    `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序序号',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_sort_order` (`sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='地区表';

-- 5. 景点分类表
CREATE TABLE IF NOT EXISTS `spot_category` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '分类ID',
    `name` VARCHAR(32) NOT NULL COMMENT '分类名称',
    `icon` VARCHAR(256) DEFAULT '' COMMENT '分类图标URL',
    `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序序号',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_sort_order` (`sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='景点分类表';

-- 6. 景点表
CREATE TABLE IF NOT EXISTS `spot` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '景点ID',
    `name` VARCHAR(64) NOT NULL COMMENT '景点名称',
    `description` TEXT COMMENT '景点描述',
    `price` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '门票价格',
    `open_time` VARCHAR(128) DEFAULT '' COMMENT '开放时间',
    `address` VARCHAR(256) DEFAULT '' COMMENT '详细地址',
    `latitude` DECIMAL(10,7) DEFAULT NULL COMMENT '纬度',
    `longitude` DECIMAL(10,7) DEFAULT NULL COMMENT '经度',
    `cover_image` VARCHAR(512) DEFAULT '' COMMENT '封面图URL',
    `category_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '分类ID',
    `region_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '地区ID',
    `heat_score` INT NOT NULL DEFAULT 0 COMMENT '热度分数',
    `avg_rating` DECIMAL(2,1) NOT NULL DEFAULT 0.0 COMMENT '平均评分',
    `rating_count` INT NOT NULL DEFAULT 0 COMMENT '评分数量',
    `published` TINYINT NOT NULL DEFAULT 0 COMMENT '发布状态：0-未发布，1-已发布',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_category_id` (`category_id`),
    KEY `idx_region_id` (`region_id`),
    KEY `idx_heat_score` (`heat_score`),
    KEY `idx_published` (`published`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='景点表';

-- 7. 景点图片表
CREATE TABLE IF NOT EXISTS `spot_image` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '图片ID',
    `spot_id` BIGINT UNSIGNED NOT NULL COMMENT '景点ID',
    `image_url` VARCHAR(512) NOT NULL COMMENT '图片URL',
    `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序序号',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_spot_id` (`spot_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='景点图片表';

-- 8. 攻略表
CREATE TABLE IF NOT EXISTS `guide` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '攻略ID',
    `title` VARCHAR(128) NOT NULL COMMENT '攻略标题',
    `content` TEXT COMMENT '攻略内容（富文本）',
    `cover_image` VARCHAR(512) DEFAULT '' COMMENT '封面图URL',
    `category` VARCHAR(32) DEFAULT '' COMMENT '攻略分类',
    `admin_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '创建管理员ID',
    `view_count` INT NOT NULL DEFAULT 0 COMMENT '浏览次数',
    `published` TINYINT NOT NULL DEFAULT 0 COMMENT '发布状态：0-未发布，1-已发布',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_category` (`category`),
    KEY `idx_published` (`published`),
    KEY `idx_view_count` (`view_count`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='攻略表';

-- 9. 攻略关联景点表
CREATE TABLE IF NOT EXISTS `guide_spot` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `guide_id` BIGINT UNSIGNED NOT NULL COMMENT '攻略ID',
    `spot_id` BIGINT UNSIGNED NOT NULL COMMENT '景点ID',
    `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序序号',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_guide_spot` (`guide_id`, `spot_id`),
    KEY `idx_guide_id` (`guide_id`),
    KEY `idx_spot_id` (`spot_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='攻略关联景点表';

-- 10. 订单表
CREATE TABLE IF NOT EXISTS `order` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '订单ID',
    `order_no` VARCHAR(32) NOT NULL COMMENT '订单编号',
    `user_id` BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    `spot_id` BIGINT UNSIGNED NOT NULL COMMENT '景点ID',
    `quantity` INT NOT NULL DEFAULT 1 COMMENT '购买数量',
    `total_amount` DECIMAL(10,2) NOT NULL COMMENT '订单总金额',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '订单状态：0-待支付，1-待使用，2-已完成，3-已取消',
    `visit_date` DATE NOT NULL COMMENT '游玩日期',
    `contact_name` VARCHAR(32) DEFAULT '' COMMENT '联系人姓名',
    `contact_phone` VARCHAR(20) DEFAULT '' COMMENT '联系人电话',
    `paid_at` DATETIME DEFAULT NULL COMMENT '支付时间',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_no` (`order_no`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_spot_id` (`spot_id`),
    KEY `idx_status` (`status`),
    KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单表';

-- 11. 评分表
CREATE TABLE IF NOT EXISTS `rating` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '评分ID',
    `user_id` BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    `spot_id` BIGINT UNSIGNED NOT NULL COMMENT '景点ID',
    `score` TINYINT NOT NULL COMMENT '评分（1-5）',
    `comment` TEXT COMMENT '评价内容',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_spot` (`user_id`, `spot_id`),
    KEY `idx_spot_id` (`spot_id`),
    KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评分表';

-- 12. 收藏表
CREATE TABLE IF NOT EXISTS `favorite` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '收藏ID',
    `user_id` BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    `spot_id` BIGINT UNSIGNED NOT NULL COMMENT '景点ID',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_spot` (`user_id`, `spot_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_spot_id` (`spot_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='收藏表';

-- 13. 轮播图表
CREATE TABLE IF NOT EXISTS `banner` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '轮播图ID',
    `image_url` VARCHAR(512) NOT NULL COMMENT '图片URL',
    `spot_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '关联景点ID',
    `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序序号',
    `enabled` TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用：0-禁用，1-启用',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_enabled_sort` (`enabled`, `sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='轮播图表';

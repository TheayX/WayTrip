-- 数据库结构脚本：用于初始化演示环境和本地开发环境的完整表结构。
-- ============================================================
-- WayTrip 数据库结构初始化脚本
-- 用途：创建可直接导入的演示/部署数据库结构
-- 字符集：utf8mb4 / utf8mb4_unicode_ci
-- ============================================================

CREATE DATABASE IF NOT EXISTS `waytrip_db`
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE `waytrip_db`;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 按依赖逆序删除，确保脚本可重复执行
DROP TABLE IF EXISTS `user_spot_view`;
DROP TABLE IF EXISTS `user_spot_review`;
DROP TABLE IF EXISTS `user_spot_favorite`;
DROP TABLE IF EXISTS `user_preference`;
DROP TABLE IF EXISTS `guide_spot_relation`;
DROP TABLE IF EXISTS `spot_image`;
DROP TABLE IF EXISTS `spot_banner`;
DROP TABLE IF EXISTS `order`;
DROP TABLE IF EXISTS `guide`;
DROP TABLE IF EXISTS `spot`;
DROP TABLE IF EXISTS `user`;
DROP TABLE IF EXISTS `admin`;
DROP TABLE IF EXISTS `spot_category`;
DROP TABLE IF EXISTS `spot_region`;

SET FOREIGN_KEY_CHECKS = 1;

CREATE TABLE `admin` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '管理员ID',
  `username` varchar(32) NOT NULL COMMENT '用户名',
  `password` varchar(128) NOT NULL COMMENT '密码（BCrypt加密）',
  `real_name` varchar(32) NOT NULL DEFAULT '' COMMENT '真实姓名',
  `is_enabled` tinyint NOT NULL DEFAULT 1 COMMENT '是否启用：0-禁用，1-启用',
  `is_deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  `last_login_at` datetime DEFAULT NULL COMMENT '最后登录时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_admin_username` (`username`),
  KEY `idx_admin_enabled_deleted` (`is_enabled`, `is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='管理员表';

CREATE TABLE `spot_region` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '地区ID',
  `parent_id` bigint unsigned NOT NULL DEFAULT 0 COMMENT '父地区ID（0表示顶级地区）',
  `name` varchar(32) NOT NULL COMMENT '地区名称',
  `sort_order` int NOT NULL DEFAULT 1 COMMENT '排序序号',
  `is_deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_region_parent_sort` (`parent_id`, `sort_order`),
  KEY `idx_region_deleted_sort` (`is_deleted`, `sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='景点地区表';

CREATE TABLE `spot_category` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '分类ID',
  `parent_id` bigint unsigned NOT NULL DEFAULT 0 COMMENT '父分类ID（0表示顶级分类）',
  `name` varchar(32) NOT NULL COMMENT '分类名称',
  `icon_url` varchar(256) NOT NULL DEFAULT '' COMMENT '分类图标URL',
  `sort_order` int NOT NULL DEFAULT 1 COMMENT '排序序号',
  `is_deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_category_parent_sort` (`parent_id`, `sort_order`),
  KEY `idx_category_deleted_sort` (`is_deleted`, `sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='景点分类表';

CREATE TABLE `user` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `openid` varchar(64) DEFAULT NULL COMMENT '微信OpenID（小程序用户）',
  `nickname` varchar(64) NOT NULL DEFAULT '' COMMENT '用户昵称',
  `phone` varchar(20) NOT NULL DEFAULT '' COMMENT '手机号',
  `password` varchar(128) DEFAULT NULL COMMENT '密码（Web端登录）',
  `avatar_url` varchar(512) NOT NULL DEFAULT '' COMMENT '头像URL',
  `is_deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  `last_login_at` datetime DEFAULT NULL COMMENT '最后登录时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_openid` (`openid`),
  UNIQUE KEY `uk_user_phone` (`phone`),
  KEY `idx_user_created_at` (`created_at`),
  KEY `idx_user_deleted_created` (`is_deleted`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

CREATE TABLE `spot` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '景点ID',
  `name` varchar(64) NOT NULL COMMENT '景点名称',
  `description` text COMMENT '景点描述',
  `price` decimal(10,2) NOT NULL DEFAULT 0.00 COMMENT '门票价格',
  `open_time` varchar(128) NOT NULL DEFAULT '' COMMENT '开放时间',
  `address` varchar(256) NOT NULL DEFAULT '' COMMENT '详细地址',
  `latitude` decimal(10,7) DEFAULT NULL COMMENT '纬度',
  `longitude` decimal(10,7) DEFAULT NULL COMMENT '经度',
  `cover_image_url` varchar(512) NOT NULL DEFAULT '' COMMENT '封面图URL',
  `category_id` bigint unsigned DEFAULT NULL COMMENT '分类ID',
  `region_id` bigint unsigned DEFAULT NULL COMMENT '地区ID',
  `heat_level` tinyint NOT NULL DEFAULT 0 COMMENT '热度档位：0-普通，1-推荐，2-重点推荐，3-强推',
  `heat_score` int NOT NULL DEFAULT 0 COMMENT '热度分数',
  `avg_rating` decimal(2,1) NOT NULL DEFAULT 0.0 COMMENT '平均评分',
  `rating_count` int NOT NULL DEFAULT 0 COMMENT '评价数量',
  `is_published` tinyint NOT NULL DEFAULT 0 COMMENT '发布状态：0-未发布，1-已发布',
  `is_deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_spot_category_id` (`category_id`),
  KEY `idx_spot_region_id` (`region_id`),
  KEY `idx_spot_heat_score` (`heat_score`),
  KEY `idx_spot_published_deleted` (`is_published`, `is_deleted`),
  KEY `idx_spot_published_deleted_heat` (`is_published`, `is_deleted`, `heat_score`, `id`),
  KEY `idx_spot_region_published` (`region_id`, `is_published`, `is_deleted`),
  CONSTRAINT `fk_spot_category` FOREIGN KEY (`category_id`) REFERENCES `spot_category` (`id`),
  CONSTRAINT `fk_spot_region` FOREIGN KEY (`region_id`) REFERENCES `spot_region` (`id`),
  CONSTRAINT `chk_spot_heat_level` CHECK (`heat_level` IN (0, 1, 2, 3)),
  CONSTRAINT `chk_spot_avg_rating` CHECK (`avg_rating` >= 0.0 AND `avg_rating` <= 5.0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='景点表';

CREATE TABLE `guide` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '攻略ID',
  `title` varchar(128) NOT NULL COMMENT '攻略标题',
  `content` mediumtext COMMENT '攻略内容（富文本，支持长文）',
  `cover_image_url` varchar(512) NOT NULL DEFAULT '' COMMENT '封面图URL',
  `category` varchar(32) NOT NULL DEFAULT '' COMMENT '攻略分类',
  `admin_id` bigint unsigned DEFAULT NULL COMMENT '创建管理员ID',
  `view_count` int NOT NULL DEFAULT 0 COMMENT '浏览次数',
  `is_published` tinyint NOT NULL DEFAULT 0 COMMENT '发布状态：0-未发布，1-已发布',
  `is_deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_guide_category` (`category`),
  KEY `idx_guide_view_count` (`view_count`),
  KEY `idx_guide_published_deleted` (`is_published`, `is_deleted`),
  KEY `idx_guide_publish_category_created` (`is_published`, `is_deleted`, `category`, `created_at`),
  KEY `idx_guide_admin_id` (`admin_id`),
  CONSTRAINT `fk_guide_admin` FOREIGN KEY (`admin_id`) REFERENCES `admin` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='攻略表';

CREATE TABLE `spot_banner` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '轮播图ID',
  `image_url` varchar(512) NOT NULL COMMENT '图片URL',
  `spot_id` bigint unsigned DEFAULT NULL COMMENT '关联景点ID',
  `sort_order` int NOT NULL DEFAULT 1 COMMENT '排序序号',
  `is_enabled` tinyint NOT NULL DEFAULT 1 COMMENT '是否启用：0-禁用，1-启用',
  `is_deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_banner_enabled_deleted_sort` (`is_enabled`, `is_deleted`, `sort_order`),
  KEY `idx_banner_spot_deleted` (`spot_id`, `is_deleted`),
  CONSTRAINT `fk_banner_spot` FOREIGN KEY (`spot_id`) REFERENCES `spot` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='景点轮播图表';

CREATE TABLE `spot_image` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '图片ID',
  `spot_id` bigint unsigned NOT NULL COMMENT '景点ID',
  `image_url` varchar(512) NOT NULL COMMENT '图片URL',
  `sort_order` int NOT NULL DEFAULT 1 COMMENT '排序序号',
  `is_deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_spot_image_spot_deleted_sort` (`spot_id`, `is_deleted`, `sort_order`),
  CONSTRAINT `fk_spot_image_spot` FOREIGN KEY (`spot_id`) REFERENCES `spot` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='景点图片表';

CREATE TABLE `guide_spot_relation` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `guide_id` bigint unsigned NOT NULL COMMENT '攻略ID',
  `spot_id` bigint unsigned NOT NULL COMMENT '景点ID',
  `sort_order` int NOT NULL DEFAULT 1 COMMENT '排序序号',
  `is_deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_guide_spot_relation` (`guide_id`, `spot_id`),
  KEY `idx_guide_spot_relation_guide_deleted_sort` (`guide_id`, `is_deleted`, `sort_order`),
  KEY `idx_guide_spot_relation_spot_deleted` (`spot_id`, `is_deleted`),
  CONSTRAINT `fk_guide_spot_relation_guide` FOREIGN KEY (`guide_id`) REFERENCES `guide` (`id`),
  CONSTRAINT `fk_guide_spot_relation_spot` FOREIGN KEY (`spot_id`) REFERENCES `spot` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='攻略景点关联表';

CREATE TABLE `user_preference` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint unsigned NOT NULL COMMENT '用户ID',
  `tag` varchar(32) NOT NULL COMMENT '偏好标签',
  `is_deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_preference_user_tag` (`user_id`, `tag`),
  KEY `idx_user_preference_user_deleted` (`user_id`, `is_deleted`),
  CONSTRAINT `fk_user_preference_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户偏好标签表';

CREATE TABLE `user_spot_favorite` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '收藏ID',
  `user_id` bigint unsigned NOT NULL COMMENT '用户ID',
  `spot_id` bigint unsigned NOT NULL COMMENT '景点ID',
  `is_deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_spot_favorite` (`user_id`, `spot_id`),
  KEY `idx_user_spot_favorite_user_deleted_created` (`user_id`, `is_deleted`, `created_at`),
  KEY `idx_user_spot_favorite_spot_deleted` (`spot_id`, `is_deleted`),
  CONSTRAINT `fk_user_spot_favorite_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`),
  CONSTRAINT `fk_user_spot_favorite_spot` FOREIGN KEY (`spot_id`) REFERENCES `spot` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='收藏表';

CREATE TABLE `user_spot_review` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '评分ID',
  `user_id` bigint unsigned NOT NULL COMMENT '用户ID',
  `spot_id` bigint unsigned NOT NULL COMMENT '景点ID',
  `score` tinyint NOT NULL COMMENT '评分（1-5）',
  `comment` text COMMENT '评论',
  `is_deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_spot_review` (`user_id`, `spot_id`),
  KEY `idx_user_spot_review_created_at` (`created_at`),
  KEY `idx_user_spot_review_spot_deleted_created` (`spot_id`, `is_deleted`, `created_at`),
  CONSTRAINT `fk_user_spot_review_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`),
  CONSTRAINT `fk_user_spot_review_spot` FOREIGN KEY (`spot_id`) REFERENCES `spot` (`id`),
  CONSTRAINT `chk_user_spot_review_score` CHECK (`score` BETWEEN 1 AND 5)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评价表';

CREATE TABLE `user_spot_view` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '浏览记录ID',
  `user_id` bigint unsigned NOT NULL COMMENT '用户ID',
  `spot_id` bigint unsigned NOT NULL COMMENT '景点ID',
  `view_source` varchar(32) NOT NULL DEFAULT 'home' COMMENT '原始来源：home/search/recommendation/guide/detail 等页面来源',
  `view_duration` int NOT NULL DEFAULT 0 COMMENT '停留时长（秒）',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_spot_view_user_spot` (`user_id`, `spot_id`),
  KEY `idx_user_spot_view_user_created_id` (`user_id`, `created_at`, `id`),
  KEY `idx_user_spot_view_spot_id` (`spot_id`),
  KEY `idx_user_spot_view_created_at` (`created_at`),
  CONSTRAINT `fk_user_spot_view_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`),
  CONSTRAINT `fk_user_spot_view_spot` FOREIGN KEY (`spot_id`) REFERENCES `spot` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户景点浏览记录表';

CREATE TABLE `order` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '订单ID',
  `order_no` varchar(32) NOT NULL COMMENT '订单编号',
  `user_id` bigint unsigned NOT NULL COMMENT '用户ID',
  `spot_id` bigint unsigned NOT NULL COMMENT '景点ID',
  `quantity` int NOT NULL DEFAULT 1 COMMENT '购买数量',
  `total_amount` decimal(10,2) NOT NULL COMMENT '订单总金额',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '订单状态：0-待支付，1-已支付，2-已取消，3-已退款，4-已完成',
  `visit_date` date NOT NULL COMMENT '游玩日期',
  `contact_name` varchar(32) NOT NULL DEFAULT '' COMMENT '联系人姓名',
  `contact_phone` varchar(20) NOT NULL DEFAULT '' COMMENT '联系人电话',
  `paid_at` datetime DEFAULT NULL COMMENT '支付时间',
  `cancelled_at` datetime DEFAULT NULL COMMENT '取消时间',
  `refunded_at` datetime DEFAULT NULL COMMENT '退款时间',
  `completed_at` datetime DEFAULT NULL COMMENT '完成时间',
  `is_deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_order_no` (`order_no`),
  KEY `idx_order_spot_id` (`spot_id`),
  KEY `idx_order_status` (`status`),
  KEY `idx_order_status_deleted_created` (`status`, `is_deleted`, `created_at`),
  KEY `idx_order_created_at` (`created_at`),
  KEY `idx_order_user_deleted_created` (`user_id`, `is_deleted`, `created_at`),
  KEY `idx_order_user_status_deleted` (`user_id`, `status`, `is_deleted`),
  KEY `idx_order_spot_status_deleted` (`spot_id`, `status`, `is_deleted`),
  KEY `idx_order_user_spot` (`user_id`, `spot_id`),
  CONSTRAINT `fk_order_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`),
  CONSTRAINT `fk_order_spot` FOREIGN KEY (`spot_id`) REFERENCES `spot` (`id`),
  CONSTRAINT `chk_order_status` CHECK (`status` IN (0, 1, 2, 3, 4))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单表';

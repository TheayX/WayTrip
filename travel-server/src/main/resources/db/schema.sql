-- MySQL dump 10.13  Distrib 8.0.36, for Win64 (x86_64)
--
-- Host: 127.0.0.1    Database: waytrip_db
-- ------------------------------------------------------
-- Server version	8.0.36

CREATE DATABASE IF NOT EXISTS waytrip_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE waytrip_db;

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `admin`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `admin` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '管理员ID',
  `username` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '用户名',
  `password` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '密码（BCrypt加密）',
  `real_name` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '真实姓名',
  `is_enabled` tinyint NOT NULL DEFAULT '1' COMMENT '是否启用：0-禁用，1-启用',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除：0-未删除，1-已删除',
  `last_login_at` datetime DEFAULT NULL COMMENT '最后登录时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='管理员表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `guide`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `guide` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '攻略ID',
  `title` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '攻略标题',
  `content` mediumtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '攻略内容（富文本，支持长文）',
  `cover_image_url` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '封面图URL',
  `category` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '攻略分类',
  `admin_id` bigint unsigned DEFAULT NULL COMMENT '创建管理员ID',
  `view_count` int NOT NULL DEFAULT '0' COMMENT '浏览次数',
  `is_published` tinyint NOT NULL DEFAULT '0' COMMENT '发布状态：0-未发布，1-已发布',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除：0-未删除，1-已删除',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_category` (`category`),
  KEY `idx_view_count` (`view_count`),
  KEY `idx_is_published` (`is_published`,`is_deleted`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='攻略表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `guide_spot_relation`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `guide_spot_relation` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `guide_id` bigint unsigned NOT NULL COMMENT '攻略ID',
  `spot_id` bigint unsigned NOT NULL COMMENT '景点ID',
  `sort_order` int NOT NULL DEFAULT '1' COMMENT '排序序号',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除：0-未删除，1-已删除',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_guide_spot` (`guide_id`,`spot_id`),
  KEY `idx_guide_id` (`guide_id`),
  KEY `idx_spot_id` (`spot_id`),
  KEY `idx_guide_id_is_deleted_sort` (`guide_id`,`is_deleted`,`sort_order`),
  KEY `idx_spot_id_is_deleted` (`spot_id`,`is_deleted`)
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='攻略景点关联表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `order`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `order` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '订单ID',
  `order_no` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '订单编号',
  `user_id` bigint unsigned NOT NULL COMMENT '用户ID',
  `spot_id` bigint unsigned NOT NULL COMMENT '景点ID',
  `quantity` int NOT NULL DEFAULT '1' COMMENT '购买数量',
  `total_amount` decimal(10,2) NOT NULL COMMENT '订单总金额',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '订单状态：0-待支付，1-已支付，2-已取消，3-已退款，4-已完成',
  `visit_date` date NOT NULL COMMENT '游玩日期',
  `contact_name` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '联系人姓名',
  `contact_phone` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '联系人电话',
  `paid_at` datetime DEFAULT NULL COMMENT '支付时间',
  `cancelled_at` datetime DEFAULT NULL COMMENT '取消时间',
  `refunded_at` datetime DEFAULT NULL COMMENT '退款时间',
  `completed_at` datetime DEFAULT NULL COMMENT '完成时间',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除：0-未删除，1-已删除',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_spot_id` (`spot_id`),
  KEY `idx_status` (`status`),
  KEY `idx_created_at` (`created_at`),
  KEY `idx_user_id_status` (`user_id`,`is_deleted`,`created_at`)
) ENGINE=InnoDB AUTO_INCREMENT=21 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `spot`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `spot` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '景点ID',
  `name` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '景点名称',
  `description` text COLLATE utf8mb4_unicode_ci COMMENT '景点描述',
  `price` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '门票价格',
  `open_time` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '开放时间',
  `address` varchar(256) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '详细地址',
  `latitude` decimal(10,7) DEFAULT NULL COMMENT '纬度',
  `longitude` decimal(10,7) DEFAULT NULL COMMENT '经度',
  `cover_image_url` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '封面图URL',
  `category_id` bigint unsigned DEFAULT NULL COMMENT '分类ID',
  `region_id` bigint unsigned DEFAULT NULL COMMENT '地区ID',
  `heat_score` int NOT NULL DEFAULT '0' COMMENT '热度分数',
  `avg_rating` decimal(2,1) NOT NULL DEFAULT '0.0' COMMENT '平均评分',
  `rating_count` int NOT NULL DEFAULT '0' COMMENT '评分数量',
  `is_published` tinyint NOT NULL DEFAULT '0' COMMENT '发布状态：0-未发布，1-已发布',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除：0-未删除，1-已删除',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_category_id` (`category_id`),
  KEY `idx_region_id` (`region_id`),
  KEY `idx_heat_score` (`heat_score`),
  KEY `idx_is_published` (`is_published`,`is_deleted`)
) ENGINE=InnoDB AUTO_INCREMENT=16 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='景点表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `spot_banner`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `spot_banner` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '轮播图ID',
  `image_url` varchar(512) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '图片URL',
  `spot_id` bigint unsigned DEFAULT NULL COMMENT '关联景点ID',
  `sort_order` int NOT NULL DEFAULT '1' COMMENT '排序序号',
  `is_enabled` tinyint NOT NULL DEFAULT '1' COMMENT '是否启用：0-禁用，1-启用',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除：0-未删除，1-已删除',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_is_enabled_sort` (`is_enabled`,`is_deleted`,`sort_order`),
  KEY `idx_spot_id_is_deleted` (`spot_id`,`is_deleted`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='景点轮播图表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `spot_category`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `spot_category` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '分类ID',
  `parent_id` bigint unsigned DEFAULT '0' COMMENT '父分类ID (0表示顶级分类)',
  `name` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '分类名称',
  `icon_url` varchar(256) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '分类图标URL',
  `sort_order` int NOT NULL DEFAULT '1' COMMENT '排序序号',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除：0-未删除，1-已删除',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_sort_order` (`sort_order`),
  KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB AUTO_INCREMENT=22 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='景点分类表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `spot_image`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `spot_image` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '图片ID',
  `spot_id` bigint unsigned NOT NULL COMMENT '景点ID',
  `image_url` varchar(512) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '图片URL',
  `sort_order` int NOT NULL DEFAULT '1' COMMENT '排序序号',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除：0-未删除，1-已删除',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_spot_id` (`spot_id`),
  KEY `idx_spot_id_is_deleted_sort` (`spot_id`,`is_deleted`,`sort_order`)
) ENGINE=InnoDB AUTO_INCREMENT=26 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='景点图片表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `spot_region`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `spot_region` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '地区ID',
  `parent_id` bigint unsigned DEFAULT '0' COMMENT '父地区ID（0表示省级）',
  `name` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '地区名称',
  `sort_order` int NOT NULL DEFAULT '1' COMMENT '排序序号',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除：0-未删除，1-已删除',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_sort_order` (`sort_order`),
  KEY `idx_parent_id` (`parent_id`),
  KEY `idx_is_deleted_sort_order` (`is_deleted`,`sort_order`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='景点地区表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `user`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `openid` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '微信OpenID',
  `nickname` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '用户昵称',
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '手机号',
  `avatar_url` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '头像URL',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除：0-未删除，1-已删除',
  `last_login_at` datetime DEFAULT NULL COMMENT '最后登录时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_openid` (`openid`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB AUTO_INCREMENT=16 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `user_preference`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_preference` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint unsigned NOT NULL COMMENT '用户ID',
  `tag` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '偏好标签',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除：0-未删除，1-已删除',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_tag` (`user_id`,`tag`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_user_id_is_deleted` (`user_id`,`is_deleted`)
) ENGINE=InnoDB AUTO_INCREMENT=23 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户偏好标签表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `user_spot_favorite`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_spot_favorite` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '收藏ID',
  `user_id` bigint unsigned NOT NULL COMMENT '用户ID',
  `spot_id` bigint unsigned NOT NULL COMMENT '景点ID',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除：0-未删除，1-已删除',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_spot` (`user_id`,`spot_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_spot_id` (`spot_id`),
  KEY `idx_user_id_is_deleted_created_at` (`user_id`,`is_deleted`,`created_at`),
  KEY `idx_spot_id_is_deleted` (`spot_id`,`is_deleted`)
) ENGINE=InnoDB AUTO_INCREMENT=42 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='收藏表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `user_spot_review`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_spot_review` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '评分ID',
  `user_id` bigint unsigned NOT NULL COMMENT '用户ID',
  `spot_id` bigint unsigned NOT NULL COMMENT '景点ID',
  `score` tinyint NOT NULL COMMENT '评分（1-5）',
  `comment` text COLLATE utf8mb4_unicode_ci COMMENT '评论',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除：0-未删除，1-已删除',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_spot` (`user_id`,`spot_id`),
  KEY `idx_created_at` (`created_at`),
  KEY `idx_spot_list` (`spot_id`,`is_deleted`,`created_at`)
) ENGINE=InnoDB AUTO_INCREMENT=61 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评价表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

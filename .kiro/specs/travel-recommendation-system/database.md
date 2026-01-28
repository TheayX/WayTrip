# 数据库设计文档

## 概述

本文档描述旅游推荐系统的数据库设计，包括表结构、索引设计、数据字典和数据库规范。数据库采用 MySQL 8.0，字符集使用 utf8mb4 以支持 emoji 等特殊字符。后端使用 MyBatis-Plus 作为 ORM 框架。

## 数据库配置

```yaml
数据库名: waytrip_db
数据库: MySQL 8.0
字符集: utf8mb4
排序规则: utf8mb4_unicode_ci
存储引擎: InnoDB
时区: Asia/Shanghai
```

## 实体关系图

```mermaid
erDiagram
    USER ||--o{ ORDER : creates
    USER ||--o{ RATING : submits
    USER ||--o{ FAVORITE : has
    USER ||--o{ USER_PREFERENCE : has
    
    SPOT ||--o{ ORDER : contains
    SPOT ||--o{ RATING : receives
    SPOT ||--o{ FAVORITE : in
    SPOT ||--o{ SPOT_IMAGE : has
    SPOT }o--|| SPOT_CATEGORY : belongs_to
    SPOT }o--|| REGION : located_in
    
    GUIDE ||--o{ GUIDE_SPOT : references
    SPOT ||--o{ GUIDE_SPOT : referenced_by
    
    ADMIN ||--o{ GUIDE : creates
    
    BANNER ||--|| SPOT : links_to
```

## 表结构设计

### 1. 用户表 (user)

存储微信小程序用户信息。

```sql
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
```

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT UNSIGNED | PK, AUTO_INCREMENT | 用户ID |
| openid | VARCHAR(64) | UNIQUE, NOT NULL | 微信OpenID |
| nickname | VARCHAR(64) | DEFAULT '' | 用户昵称 |
| avatar | VARCHAR(512) | DEFAULT '' | 头像URL |
| created_at | DATETIME | NOT NULL | 创建时间 |
| updated_at | DATETIME | NOT NULL | 更新时间 |

### 2. 用户偏好标签表 (user_preference)

存储用户选择的偏好标签，用于冷启动推荐。

```sql
CREATE TABLE IF NOT EXISTS `user_preference` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    `tag` VARCHAR(32) NOT NULL COMMENT '偏好标签',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_tag` (`user_id`, `tag`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户偏好标签表';
```

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT UNSIGNED | PK, AUTO_INCREMENT | 主键ID |
| user_id | BIGINT UNSIGNED | NOT NULL | 用户ID |
| tag | VARCHAR(32) | NOT NULL | 偏好标签（如：自然风光、历史文化） |
| created_at | DATETIME | NOT NULL | 创建时间 |

### 3. 管理员表 (admin)

存储管理后台管理员账号。

```sql
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
```

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT UNSIGNED | PK, AUTO_INCREMENT | 管理员ID |
| username | VARCHAR(32) | UNIQUE, NOT NULL | 用户名 |
| password | VARCHAR(128) | NOT NULL | 密码（BCrypt加密） |
| real_name | VARCHAR(32) | DEFAULT '' | 真实姓名 |
| status | TINYINT | NOT NULL, DEFAULT 1 | 状态：0-禁用，1-启用 |
| last_login_at | DATETIME | NULL | 最后登录时间 |
| created_at | DATETIME | NOT NULL | 创建时间 |
| updated_at | DATETIME | NOT NULL | 更新时间 |

### 4. 地区表 (region)

存储景点所属地区。

```sql
CREATE TABLE IF NOT EXISTS `region` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '地区ID',
    `name` VARCHAR(32) NOT NULL COMMENT '地区名称',
    `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序序号',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_sort_order` (`sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='地区表';
```

### 5. 景点分类表 (spot_category)

存储景点分类/主题。

```sql
CREATE TABLE IF NOT EXISTS `spot_category` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '分类ID',
    `name` VARCHAR(32) NOT NULL COMMENT '分类名称',
    `icon` VARCHAR(256) DEFAULT '' COMMENT '分类图标URL',
    `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序序号',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_sort_order` (`sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='景点分类表';
```

### 6. 景点表 (spot)

存储景点核心信息。

```sql
CREATE TABLE IF NOT EXISTS `spot` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '景点ID',
    `name` VARCHAR(64) NOT NULL COMMENT '景点名称',
    `description` TEXT COMMENT '景点简介',
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
    KEY `idx_published` (`published`),
    KEY `idx_heat_score` (`heat_score`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='景点表';
```

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT UNSIGNED | PK, AUTO_INCREMENT | 景点ID |
| name | VARCHAR(64) | NOT NULL | 景点名称 |
| description | TEXT | NULL | 景点简介 |
| price | DECIMAL(10,2) | NOT NULL, DEFAULT 0.00 | 门票价格 |
| open_time | VARCHAR(128) | DEFAULT '' | 开放时间 |
| address | VARCHAR(256) | DEFAULT '' | 详细地址 |
| latitude | DECIMAL(10,7) | NULL | 纬度 |
| longitude | DECIMAL(10,7) | NULL | 经度 |
| cover_image | VARCHAR(512) | DEFAULT '' | 封面图URL |
| category_id | BIGINT UNSIGNED | NULL, FK | 分类ID |
| region_id | BIGINT UNSIGNED | NULL, FK | 地区ID |
| heat_score | INT | NOT NULL, DEFAULT 0 | 热度分数 |
| avg_rating | DECIMAL(2,1) | NOT NULL, DEFAULT 0.0 | 平均评分 |
| rating_count | INT | NOT NULL, DEFAULT 0 | 评分数量 |
| published | TINYINT | NOT NULL, DEFAULT 0 | 发布状态 |
| created_at | DATETIME | NOT NULL | 创建时间 |
| updated_at | DATETIME | NOT NULL | 更新时间 |

### 7. 景点图片表 (spot_image)

存储景点的多张图片。

```sql
CREATE TABLE IF NOT EXISTS `spot_image` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '图片ID',
    `spot_id` BIGINT UNSIGNED NOT NULL COMMENT '景点ID',
    `image_url` VARCHAR(512) NOT NULL COMMENT '图片URL',
    `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序序号',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_spot_id` (`spot_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='景点图片表';
```


### 8. 攻略表 (guide)

存储旅游攻略内容。

```sql
CREATE TABLE IF NOT EXISTS `guide` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '攻略ID',
    `title` VARCHAR(128) NOT NULL COMMENT '攻略标题',
    `content` TEXT COMMENT '攻略内容（富文本HTML）',
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
```

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT UNSIGNED | PK, AUTO_INCREMENT | 攻略ID |
| title | VARCHAR(128) | NOT NULL | 攻略标题 |
| content | TEXT | NULL | 攻略内容（富文本HTML） |
| cover_image | VARCHAR(512) | DEFAULT '' | 封面图URL |
| category | VARCHAR(32) | DEFAULT '' | 攻略分类 |
| admin_id | BIGINT UNSIGNED | NULL, FK | 创建管理员ID |
| view_count | INT | NOT NULL, DEFAULT 0 | 浏览次数 |
| published | TINYINT | NOT NULL, DEFAULT 0 | 发布状态 |
| created_at | DATETIME | NOT NULL | 创建时间 |
| updated_at | DATETIME | NOT NULL | 更新时间 |

### 9. 攻略关联景点表 (guide_spot)

存储攻略与景点的关联关系。

```sql
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
```

### 10. 订单表 (order)

存储用户订单信息。

```sql
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
```

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT UNSIGNED | PK, AUTO_INCREMENT | 订单ID |
| order_no | VARCHAR(32) | UNIQUE, NOT NULL | 订单编号 |
| user_id | BIGINT UNSIGNED | NOT NULL, FK | 用户ID |
| spot_id | BIGINT UNSIGNED | NOT NULL, FK | 景点ID |
| quantity | INT | NOT NULL, DEFAULT 1 | 购买数量 |
| total_amount | DECIMAL(10,2) | NOT NULL | 订单总金额 |
| status | TINYINT | NOT NULL, DEFAULT 0 | 订单状态 |
| visit_date | DATE | NOT NULL | 游玩日期 |
| contact_name | VARCHAR(32) | DEFAULT '' | 联系人姓名 |
| contact_phone | VARCHAR(20) | DEFAULT '' | 联系人电话 |
| paid_at | DATETIME | NULL | 支付时间 |
| created_at | DATETIME | NOT NULL | 创建时间 |
| updated_at | DATETIME | NOT NULL | 更新时间 |

**订单状态枚举值：**
- `0` - 待支付 (PENDING_PAYMENT)
- `1` - 待使用 (PENDING_USE)
- `2` - 已完成 (COMPLETED)
- `3` - 已取消 (CANCELLED)

### 11. 评分评论表 (rating)

存储用户对景点的评分和评论。

```sql
CREATE TABLE IF NOT EXISTS `rating` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '评分ID',
    `user_id` BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    `spot_id` BIGINT UNSIGNED NOT NULL COMMENT '景点ID',
    `score` TINYINT NOT NULL COMMENT '评分（1-5）',
    `comment` TEXT COMMENT '评论内容',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_spot` (`user_id`, `spot_id`),
    KEY `idx_spot_id` (`spot_id`),
    KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评分评论表';
```

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT UNSIGNED | PK, AUTO_INCREMENT | 评分ID |
| user_id | BIGINT UNSIGNED | NOT NULL, FK | 用户ID |
| spot_id | BIGINT UNSIGNED | NOT NULL, FK | 景点ID |
| score | TINYINT | NOT NULL | 评分（1-5） |
| comment | TEXT | NULL | 评论内容 |
| created_at | DATETIME | NOT NULL | 创建时间 |
| updated_at | DATETIME | NOT NULL | 更新时间 |

### 12. 收藏表 (favorite)

存储用户收藏的景点。

```sql
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
```

### 13. 轮播图表 (banner)

存储首页轮播图配置。

```sql
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
```

## 索引设计说明

### 索引策略

| 表名 | 索引名 | 索引类型 | 字段 | 用途 |
|------|--------|----------|------|------|
| user | uk_openid | UNIQUE | openid | 微信登录查询 |
| spot | idx_published | BTREE | published | 发布状态过滤 |
| spot | idx_heat_score | BTREE | heat_score | 热门排序 |
| order | uk_order_no | UNIQUE | order_no | 订单号查询 |
| order | idx_user_id | BTREE | user_id | 用户订单列表 |
| rating | uk_user_spot | UNIQUE | user_id, spot_id | 防止重复评分 |
| favorite | uk_user_spot | UNIQUE | user_id, spot_id | 防止重复收藏 |

## Redis 缓存设计

### 缓存 Key 设计

| Key 格式 | 数据类型 | 过期时间 | 说明 |
|----------|----------|----------|------|
| `user:token:{token}` | String | 7天 | 用户登录Token |
| `admin:token:{token}` | String | 24小时 | 管理员登录Token |
| `spot:detail:{spotId}` | Hash | 1小时 | 景点详情缓存 |
| `spot:hot:list` | ZSet | 10分钟 | 热门景点列表 |
| `item:similarity:{spotId}` | Hash | 24小时 | 物品相似度矩阵 |
| `user:rec:{userId}` | List | 30分钟 | 用户推荐结果缓存 |
| `banner:list` | List | 5分钟 | 轮播图列表 |

## 数据库初始化脚本

### 基础数据

```sql
-- 地区数据
INSERT INTO `region` (`name`, `sort_order`) VALUES
('北京', 1),
('上海', 2),
('广州', 3),
('深圳', 4),
('杭州', 5),
('成都', 6),
('西安', 7),
('重庆', 8);

-- 景点分类数据
INSERT INTO `spot_category` (`name`, `sort_order`) VALUES
('自然风光', 1),
('历史文化', 2),
('主题乐园', 3),
('城市观光', 4),
('休闲度假', 5),
('户外探险', 6);

-- 默认管理员账号（密码: admin123，使用BCrypt加密）
INSERT INTO `admin` (`username`, `password`, `real_name`, `status`) VALUES
('admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZRGdjGj/n3.rsQ0fEnxRiT1QXqAAK', '系统管理员', 1);
```

## 数据库规范

### 命名规范

1. **表名**：小写下划线命名，如 `user`、`spot_category`
2. **字段名**：小写下划线命名，如 `created_at`
3. **索引名**：
   - 主键：`PRIMARY`
   - 唯一索引：`uk_` 前缀
   - 普通索引：`idx_` 前缀

### 字段规范

1. **主键**：统一使用 `BIGINT UNSIGNED AUTO_INCREMENT`
2. **时间字段**：统一使用 `DATETIME` 类型
3. **金额字段**：统一使用 `DECIMAL(10,2)`
4. **状态字段**：使用 `TINYINT`
5. **URL字段**：使用 `VARCHAR(512)`
6. **长文本**：使用 `TEXT`

### 外键约束

本设计不使用数据库外键约束，通过应用层保证数据一致性，原因：
1. 提高写入性能
2. 便于分库分表扩展
3. 简化数据迁移

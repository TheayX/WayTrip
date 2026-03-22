# 数据库设计文档

## 文档说明

- 对齐基线：`travel-server/src/main/resources/db/schema.sql`
- 更新时间：2026-03-22
- 说明：本版按当前建表脚本整理，不再沿用历史推导字段说明

## 数据库基础信息

| 项 | 值 |
| --- | --- |
| 数据库名 | `waytrip_db` |
| 数据库 | MySQL 8.0 |
| 字符集 | `utf8mb4` |
| 排序规则 | `utf8mb4_unicode_ci` |
| 存储引擎 | InnoDB |

## 表总览

当前共有 13 张核心业务表：

1. `user`
2. `user_preference`
3. `admin`
4. `spot_region`
5. `spot_category`
6. `spot`
7. `spot_image`
8. `guide`
9. `guide_spot_relation`
10. `order`
11. `user_spot_review`
12. `user_spot_favorite`
13. `spot_banner`

## 关系摘要

```mermaid
erDiagram
    USER ||--o{ ORDER : creates
    USER ||--o{ USER_SPOT_REVIEW : writes
    USER ||--o{ USER_SPOT_FAVORITE : keeps
    USER ||--o{ USER_PREFERENCE : sets

    SPOT ||--o{ ORDER : belongs_to
    SPOT ||--o{ SPOT_IMAGE : has
    SPOT ||--o{ USER_SPOT_REVIEW : receives
    SPOT ||--o{ USER_SPOT_FAVORITE : receives
    SPOT }o--|| SPOT_REGION : in
    SPOT }o--|| SPOT_CATEGORY : in

    GUIDE ||--o{ GUIDE_SPOT_RELATION : maps
    SPOT ||--o{ GUIDE_SPOT_RELATION : maps
    ADMIN ||--o{ GUIDE : creates
    SPOT_BANNER }o--o| SPOT : links
```

## 表结构摘要

### 1. `user`

用途：用户主表，兼容小程序 OpenID 与 Web 手机号密码账户。

关键字段：

- `openid`
- `phone`
- `password`
- `avatar_url`
- `is_deleted`
- `last_login_at`

约束：

- `uk_openid`
- `uk_phone`

### 2. `user_preference`

用途：用户偏好标签。

关键字段：

- `user_id`
- `tag`
- `is_deleted`

约束：

- `uk_user_tag(user_id, tag)`

### 3. `admin`

用途：后台管理员账户。

关键字段：

- `username`
- `password`
- `real_name`
- `is_enabled`
- `is_deleted`
- `last_login_at`

约束：

- `uk_username`

### 4. `spot_region`

用途：景点地区树，当前为二级结构。

关键字段：

- `parent_id`
- `name`
- `sort_order`
- `is_deleted`

### 5. `spot_category`

用途：景点分类树。

关键字段：

- `parent_id`
- `name`
- `icon_url`
- `sort_order`
- `is_deleted`

### 6. `spot`

用途：景点主表。

关键字段：

- `name`
- `description`
- `price`
- `open_time`
- `address`
- `latitude`
- `longitude`
- `cover_image_url`
- `category_id`
- `region_id`
- `heat_score`
- `avg_rating`
- `rating_count`
- `is_published`
- `is_deleted`

### 7. `spot_image`

用途：景点详情图片。

关键字段：

- `spot_id`
- `image_url`
- `sort_order`
- `is_deleted`

### 8. `guide`

用途：攻略主表。

关键字段：

- `title`
- `content`
- `cover_image_url`
- `category`
- `admin_id`
- `view_count`
- `is_published`
- `is_deleted`

### 9. `guide_spot_relation`

用途：攻略与景点关系表。

关键字段：

- `guide_id`
- `spot_id`
- `sort_order`
- `is_deleted`

约束：

- `uk_guide_spot(guide_id, spot_id)`

### 10. `order`

用途：用户订单。

关键字段：

- `order_no`
- `user_id`
- `spot_id`
- `quantity`
- `total_amount`
- `status`
- `visit_date`
- `contact_name`
- `contact_phone`
- `paid_at`
- `cancelled_at`
- `refunded_at`
- `completed_at`
- `is_deleted`

约束：

- `uk_order_no`

状态值：

| 值 | 含义 |
| --- | --- |
| 0 | 待支付 |
| 1 | 已支付 |
| 2 | 已取消 |
| 3 | 已退款 |
| 4 | 已完成 |

### 11. `user_spot_review`

用途：景点评分评论。

关键字段：

- `user_id`
- `spot_id`
- `score`
- `comment`
- `is_deleted`

约束：

- `uk_user_spot(user_id, spot_id)`，保证单用户单景点唯一评分记录

### 12. `user_spot_favorite`

用途：景点收藏。

关键字段：

- `user_id`
- `spot_id`
- `is_deleted`

约束：

- `uk_user_spot(user_id, spot_id)`，防止重复收藏记录

### 13. `spot_banner`

用途：首页轮播图配置。

关键字段：

- `image_url`
- `spot_id`
- `sort_order`
- `is_enabled`
- `is_deleted`

## 当前索引设计重点

| 表 | 关键索引 | 用途 |
| --- | --- | --- |
| `user` | `uk_openid`, `uk_phone` | 登录与绑定查找 |
| `spot` | `idx_category_id`, `idx_region_id`, `idx_heat_score`, `idx_is_published` | 列表筛选排序 |
| `guide` | `idx_category`, `idx_view_count`, `idx_is_published` | 攻略筛选与展示 |
| `order` | `uk_order_no`, `idx_status`, `idx_user_id_status` | 订单详情、订单列表 |
| `user_spot_review` | `uk_user_spot`, `idx_spot_list` | 评分去重、评论列表 |
| `user_spot_favorite` | `uk_user_spot`, `idx_user_id_is_deleted_created_at` | 收藏去重、收藏列表 |
| `spot_banner` | `idx_is_enabled_sort` | 首页轮播图读取 |
| `guide_spot_relation` | `uk_guide_spot`, `idx_guide_id_is_deleted_sort` | 关联景点读取 |

## Redis 使用现状

当前 Redis 主要用于推荐系统：

- 存储景点相似度矩阵
- 为推荐结果提供缓存/中间存储支持

说明：

- 文档中的“热点缓存、轮播缓存、详情缓存”属于规划项，不应视为全部已落地。
- 当前确认已实现的是推荐矩阵相关 Redis 使用。

## 数据设计约定

### 通用约定

1. 主键统一使用 `BIGINT UNSIGNED AUTO_INCREMENT`
2. 金额统一使用 `DECIMAL(10,2)`
3. 状态字段统一使用 `TINYINT`
4. URL 字段统一使用 `VARCHAR`
5. 长文本内容使用 `TEXT` 或 `MEDIUMTEXT`
6. 不使用数据库外键，关联一致性由应用层保证

### 逻辑删除

所有核心表都带 `is_deleted`，业务读取默认应过滤已删除数据。

### 时间字段

当前建表脚本中多数表的 `updated_at` 使用 `DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP`。

说明：

- 历史文档曾将多张表写成 `ON UPDATE CURRENT_TIMESTAMP`，与实际脚本不完全一致。
- 如果后续需要严格依赖数据库自动更新时间，应先确认实体更新策略与建表脚本是否同步。

## 当前文档化结论

- 表结构主体已经稳定，文档此前最大的偏差不在表数量，而在字段默认值和 `updated_at` 行为描述。
- 当前数据库文档应以 `schema.sql` 为唯一真实来源。

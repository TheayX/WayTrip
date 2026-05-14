-- 扩容数据脚本：用于向现有库追加用户偏好数据。
-- ============================================================
-- WayTrip 手工扩容用户偏好数据
-- 目标：为扩容用户补充基础兴趣标签，支撑冷启动推荐和后续行为分布
-- 说明：仅追加新数据，不清空、不覆盖、不修改已有基础数据
-- 依赖：需先执行 db/data.sql 与 db/seed/bulk/20_user.sql
-- 备注：依赖 MySQL 8 递归 CTE；重复执行会因主键或唯一键冲突而失败
-- ============================================================

USE `waytrip_db`;

SET NAMES utf8mb4;

INSERT INTO `user_preference`
  (`id`, `user_id`, `tag`, `is_deleted`, `created_at`, `updated_at`)
WITH RECURSIVE seq AS (
  SELECT 1 AS n
  UNION ALL
  SELECT n + 1 FROM seq WHERE n < 120
),
pref_seed AS (
  SELECT
    n,
    10000 + n AS user_id,
    20000 + (n - 1) * 2 + 1 AS first_pref_id,
    20000 + (n - 1) * 2 + 2 AS second_pref_id,
    CAST(7 + MOD(n - 1, 14) AS CHAR) AS first_tag,
    CAST(7 + MOD(n + 4, 14) AS CHAR) AS second_tag,
    DATE_ADD('2025-09-15 10:00:00', INTERVAL n DAY) AS created_at
  FROM seq
)
SELECT
  first_pref_id AS `id`,
  user_id,
  first_tag AS `tag`,
  0 AS `is_deleted`,
  created_at,
  created_at AS `updated_at`
FROM pref_seed
UNION ALL
SELECT
  second_pref_id AS `id`,
  user_id,
  second_tag AS `tag`,
  0 AS `is_deleted`,
  DATE_ADD(created_at, INTERVAL 5 MINUTE) AS `created_at`,
  DATE_ADD(created_at, INTERVAL 5 MINUTE) AS `updated_at`
FROM pref_seed;

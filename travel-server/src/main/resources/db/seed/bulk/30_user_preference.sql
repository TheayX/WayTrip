-- 扩容数据脚本：用于向现有库追加用户偏好数据。
-- ============================================================
-- WayTrip 手工扩容用户偏好数据
-- 目标：为扩容用户补充基础兴趣标签，支撑冷启动推荐和后续行为分布
-- 说明：仅追加新数据，不清空、不覆盖、不修改已有基础数据
-- 依赖：需先执行 db/data.sql、db/seed/bulk/10_spot.sql 与 db/seed/bulk/20_user.sql
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
category_seed AS (
  SELECT 1 AS rn, '7' AS tag UNION ALL
  SELECT 2, '8' UNION ALL
  SELECT 3, '9' UNION ALL
  SELECT 4, '10' UNION ALL
  SELECT 5, '11' UNION ALL
  SELECT 6, '12' UNION ALL
  SELECT 7, '13' UNION ALL
  SELECT 8, '14' UNION ALL
  SELECT 9, '15' UNION ALL
  SELECT 10, '16' UNION ALL
  SELECT 11, '17' UNION ALL
  SELECT 12, '18' UNION ALL
  SELECT 13, '19' UNION ALL
  SELECT 14, '20' UNION ALL
  SELECT 15, '21' UNION ALL
  SELECT 16, '22' UNION ALL
  SELECT 17, '23' UNION ALL
  SELECT 18, '24' UNION ALL
  SELECT 19, '25' UNION ALL
  SELECT 20, '26' UNION ALL
  SELECT 21, '27' UNION ALL
  SELECT 22, '28' UNION ALL
  SELECT 23, '29' UNION ALL
  SELECT 24, '30' UNION ALL
  SELECT 25, '31' UNION ALL
  SELECT 26, '32' UNION ALL
  SELECT 27, '33' UNION ALL
  SELECT 28, '34' UNION ALL
  SELECT 29, '35' UNION ALL
  SELECT 30, '36' UNION ALL
  SELECT 31, '37' UNION ALL
  SELECT 32, '38' UNION ALL
  SELECT 33, '39' UNION ALL
  SELECT 34, '40' UNION ALL
  SELECT 35, '41' UNION ALL
  SELECT 36, '42' UNION ALL
  SELECT 37, '43'
),
pref_seed AS (
  SELECT
    n,
    10000 + n AS user_id,
    20000 + (n - 1) * 2 + 1 AS first_pref_id,
    20000 + (n - 1) * 2 + 2 AS second_pref_id,
    (SELECT tag FROM category_seed WHERE rn = 1 + MOD(n - 1, 37)) AS first_tag,
    (SELECT tag FROM category_seed WHERE rn = 1 + MOD(n + 10, 37)) AS second_tag,
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

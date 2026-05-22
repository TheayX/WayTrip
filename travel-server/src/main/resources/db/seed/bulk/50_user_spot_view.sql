-- 扩容数据脚本：用于向现有库追加用户浏览记录数据。
-- ============================================================
-- WayTrip 手工扩容用户浏览数据
-- 目标：为推荐、最近浏览和行为分析提供更充足的基础浏览样本
-- 说明：仅追加新数据，不清空、不覆盖、不修改已有基础数据
-- 依赖：需先执行 db/data.sql、db/seed/bulk/10_spot.sql、db/seed/bulk/20_user.sql
-- 备注：依赖 MySQL 8 递归 CTE；重复执行会因主键冲突而失败
-- ============================================================

USE `waytrip_db`;

SET NAMES utf8mb4;

INSERT INTO `user_spot_view`
  (`id`, `user_id`, `spot_id`, `view_source`, `view_duration`, `created_at`)
WITH RECURSIVE user_seq AS (
  SELECT 1 AS n
  UNION ALL
  SELECT n + 1 FROM user_seq WHERE n < 120
),
view_slot AS (
  SELECT 1 AS slot
  UNION ALL
  SELECT slot + 1 FROM view_slot WHERE slot < 20
),
view_seed AS (
  SELECT
    user_seq.n,
    view_slot.slot,
    CASE
      WHEN MOD(user_seq.n, 15) IN (0, 1) THEN 20
      WHEN MOD(user_seq.n, 15) IN (2, 3, 4) THEN 15
      WHEN MOD(user_seq.n, 15) IN (5, 6, 7, 8, 9) THEN 10
      ELSE 6
    END AS view_count
  FROM user_seq
  CROSS JOIN view_slot
)
SELECT
  40000 + (view_seed.n - 1) * 20 + view_seed.slot AS `id`,
  10000 + view_seed.n AS `user_id`,
  CASE
    WHEN view_seed.slot BETWEEN 1 AND 2 THEN ELT(1 + MOD(view_seed.n + view_seed.slot - 1, 8), 1, 3, 5, 7, 9, 11, 14, 16)
    WHEN view_seed.slot BETWEEN 3 AND 5 THEN ELT(1 + MOD(view_seed.n + view_seed.slot, 10), 1002, 1004, 1006, 1011, 1013, 1017, 1021, 1031, 1045, 1058)
    WHEN view_seed.slot BETWEEN 6 AND 8 THEN 1 + MOD(view_seed.n + view_seed.slot - 3, 16)
    WHEN view_seed.slot BETWEEN 9 AND 14 THEN 1001 + MOD(view_seed.n + view_seed.slot + 7, 60)
    ELSE ELT(1 + MOD(view_seed.n + view_seed.slot + 2, 10), 1008, 1010, 1014, 1018, 1023, 1025, 1034, 1041, 1048, 1060)
  END AS `spot_id`,
  CASE MOD(view_seed.slot, 5)
    WHEN 1 THEN 'home'
    WHEN 2 THEN 'search'
    WHEN 3 THEN 'recommendation'
    WHEN 4 THEN 'guide'
    ELSE 'detail'
  END AS `view_source`,
  35 + MOD(view_seed.n * 17 + view_seed.slot * 23, 386) AS `view_duration`,
  DATE_ADD(
    DATE_ADD('2026-03-01 08:00:00', INTERVAL view_seed.n + MOD(view_seed.n, 9) + FLOOR((view_seed.slot - 1) / 3) DAY),
    INTERVAL MOD(view_seed.n * 29 + view_seed.slot * 41, 1320) MINUTE
  ) AS `created_at`
FROM view_seed
WHERE view_seed.slot <= view_seed.view_count;

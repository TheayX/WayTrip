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
  SELECT slot + 1 FROM view_slot WHERE slot < 12
)
SELECT
  40000 + (user_seq.n - 1) * 12 + view_slot.slot AS `id`,
  10000 + user_seq.n AS `user_id`,
  CASE
    WHEN view_slot.slot BETWEEN 1 AND 3 THEN 1 + MOD(user_seq.n + view_slot.slot - 2, 16)
    WHEN view_slot.slot BETWEEN 4 AND 8 THEN 1001 + MOD(user_seq.n + view_slot.slot + 7, 60)
    ELSE 1001 + MOD(user_seq.n + view_slot.slot + 27, 60)
  END AS `spot_id`,
  CASE MOD(view_slot.slot, 5)
    WHEN 1 THEN 'home'
    WHEN 2 THEN 'search'
    WHEN 3 THEN 'recommendation'
    WHEN 4 THEN 'guide'
    ELSE 'detail'
  END AS `view_source`,
  60 + MOD(user_seq.n * 17 + view_slot.slot * 23, 241) AS `view_duration`,
  DATE_ADD('2026-03-01 08:00:00', INTERVAL user_seq.n * 3 + view_slot.slot DAY) AS `created_at`
FROM user_seq
CROSS JOIN view_slot;

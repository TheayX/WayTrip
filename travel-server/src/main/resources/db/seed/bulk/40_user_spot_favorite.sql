-- 扩容数据脚本：用于向现有库追加用户收藏数据。
-- ============================================================
-- WayTrip 手工扩容用户收藏数据
-- 目标：为扩容用户和扩容景点建立第一层稳定互动关系
-- 说明：仅追加新数据，不清空、不覆盖、不修改已有基础数据
-- 依赖：需先执行 db/data.sql、db/seed/bulk/10_spot.sql、db/seed/bulk/20_user.sql
-- 备注：依赖 MySQL 8 递归 CTE；重复执行会因主键或唯一键冲突而失败
-- ============================================================

USE `waytrip_db`;

SET NAMES utf8mb4;

INSERT INTO `user_spot_favorite`
  (`id`, `user_id`, `spot_id`, `is_deleted`, `created_at`, `updated_at`)
WITH RECURSIVE user_seq AS (
  SELECT 1 AS n
  UNION ALL
  SELECT n + 1 FROM user_seq WHERE n < 120
),
fav_slot AS (
  SELECT 1 AS slot
  UNION ALL
  SELECT slot + 1 FROM fav_slot WHERE slot < 3
)
SELECT
  30000 + (user_seq.n - 1) * 3 + fav_slot.slot AS `id`,
  10000 + user_seq.n AS `user_id`,
  CASE fav_slot.slot
    WHEN 1 THEN 1 + MOD(user_seq.n - 1, 16)
    WHEN 2 THEN 1001 + MOD(user_seq.n + 11, 60)
    ELSE 1001 + MOD(user_seq.n + 27, 60)
  END AS `spot_id`,
  0 AS `is_deleted`,
  DATE_ADD('2026-02-01 10:00:00', INTERVAL user_seq.n * 2 + fav_slot.slot DAY) AS `created_at`,
  DATE_ADD('2026-02-01 10:00:00', INTERVAL user_seq.n * 2 + fav_slot.slot DAY) AS `updated_at`
FROM user_seq
CROSS JOIN fav_slot;

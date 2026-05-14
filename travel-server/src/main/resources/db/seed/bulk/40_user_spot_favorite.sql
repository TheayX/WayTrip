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
  SELECT slot + 1 FROM fav_slot WHERE slot < 6
),
favorite_seed AS (
  SELECT
    user_seq.n,
    fav_slot.slot,
    CASE
      WHEN MOD(user_seq.n, 12) IN (0, 1) THEN 6
      WHEN MOD(user_seq.n, 12) IN (2, 3, 4) THEN 4
      WHEN MOD(user_seq.n, 12) IN (5, 6, 7, 8) THEN 3
      ELSE 1
    END AS favorite_count
  FROM user_seq
  CROSS JOIN fav_slot
)
SELECT
  30000 + (favorite_seed.n - 1) * 6 + favorite_seed.slot AS `id`,
  10000 + favorite_seed.n AS `user_id`,
  CASE favorite_seed.slot
    WHEN 1 THEN ELT(1 + MOD(favorite_seed.n - 1, 8), 1, 3, 5, 7, 9, 11, 14, 16)
    WHEN 2 THEN ELT(1 + MOD(favorite_seed.n + 1, 10), 1002, 1004, 1006, 1011, 1013, 1017, 1021, 1031, 1045, 1058)
    WHEN 3 THEN 1001 + MOD(favorite_seed.n + 17, 60)
    WHEN 4 THEN ELT(1 + MOD(favorite_seed.n + 2, 8), 2, 4, 6, 8, 10, 12, 13, 15)
    WHEN 5 THEN ELT(1 + MOD(favorite_seed.n + 3, 10), 1008, 1010, 1014, 1018, 1023, 1025, 1034, 1041, 1048, 1060)
    ELSE 1001 + MOD(favorite_seed.n + 41, 60)
  END AS `spot_id`,
  0 AS `is_deleted`,
  DATE_ADD(
    DATE_ADD('2026-02-01 10:00:00', INTERVAL favorite_seed.n + favorite_seed.slot + MOD(favorite_seed.n, 11) DAY),
    INTERVAL MOD(favorite_seed.n * 37 + favorite_seed.slot * 19, 780) MINUTE
  ) AS `created_at`,
  DATE_ADD(
    DATE_ADD('2026-02-01 10:00:00', INTERVAL favorite_seed.n + favorite_seed.slot + MOD(favorite_seed.n, 11) DAY),
    INTERVAL MOD(favorite_seed.n * 37 + favorite_seed.slot * 19, 780) MINUTE
  ) AS `updated_at`
FROM favorite_seed
WHERE favorite_seed.slot <= favorite_seed.favorite_count;

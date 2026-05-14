-- 扩容数据脚本：用于向现有库追加用户评论数据。
-- ============================================================
-- WayTrip 手工扩容用户评论数据
-- 目标：补充评分和评论文本样本，支撑评分展示、口碑列表和推荐算法
-- 说明：仅追加新数据，不清空、不覆盖、不修改已有基础数据
-- 依赖：需先执行 db/data.sql、db/seed/bulk/10_spot.sql、db/seed/bulk/20_user.sql
-- 备注：依赖 MySQL 8 递归 CTE；重复执行会因主键或唯一键冲突而失败
-- ============================================================

USE `waytrip_db`;

SET NAMES utf8mb4;

INSERT INTO `user_spot_review`
  (`id`, `user_id`, `spot_id`, `score`, `comment`, `is_deleted`, `created_at`, `updated_at`)
WITH RECURSIVE user_seq AS (
  SELECT 1 AS n
  UNION ALL
  SELECT n + 1 FROM user_seq WHERE n < 120
),
review_slot AS (
  SELECT 1 AS slot
  UNION ALL
  SELECT slot + 1 FROM review_slot WHERE slot < 3
),
review_seed AS (
  SELECT
    user_seq.n,
    review_slot.slot,
    CASE
      WHEN MOD(user_seq.n, 18) IN (0, 1) THEN 3
      WHEN MOD(user_seq.n, 18) IN (2, 3, 4, 5, 6, 7) THEN 2
      WHEN MOD(user_seq.n, 18) IN (8, 9, 10, 11, 12) THEN 1
      ELSE 0
    END AS review_count
  FROM user_seq
  CROSS JOIN review_slot
)
SELECT
  50000 + (review_seed.n - 1) * 3 + review_seed.slot AS `id`,
  10000 + review_seed.n AS `user_id`,
  CASE review_seed.slot
    WHEN 1 THEN ELT(1 + MOD(review_seed.n + 1, 8), 1, 3, 5, 7, 9, 11, 14, 16)
    WHEN 2 THEN ELT(1 + MOD(review_seed.n + 3, 10), 1002, 1004, 1006, 1011, 1013, 1017, 1021, 1031, 1045, 1058)
    ELSE 1001 + MOD(review_seed.n + 37, 60)
  END AS `spot_id`,
  CASE MOD(review_seed.n + review_seed.slot, 10)
    WHEN 0 THEN 5
    WHEN 1 THEN 4
    WHEN 2 THEN 5
    WHEN 3 THEN 4
    WHEN 4 THEN 5
    WHEN 5 THEN 4
    WHEN 6 THEN 3
    WHEN 7 THEN 5
    WHEN 8 THEN 4
    ELSE 2
  END AS `score`,
  CASE MOD(review_seed.n + review_seed.slot, 6)
    WHEN 0 THEN '整体体验顺畅，线路安排合理的话会更舒服。'
    WHEN 1 THEN '景点辨识度不错，适合放进城市核心行程里。'
    WHEN 2 THEN '现场氛围比预期稳定，错峰过去体验更好。'
    WHEN 3 THEN '适合慢慢逛，不建议压缩成匆忙打卡。'
    WHEN 4 THEN '和周边景点联动性不错，安排半天到一天都合适。'
    ELSE '整体值得去，拍照和步行体验都比较在线。'
  END AS `comment`,
  0 AS `is_deleted`,
  DATE_ADD(
    DATE_ADD('2026-03-15 09:00:00', INTERVAL review_seed.n + MOD(review_seed.n, 7) + review_seed.slot DAY),
    INTERVAL MOD(review_seed.n * 31 + review_seed.slot * 17, 1080) MINUTE
  ) AS `created_at`,
  DATE_ADD(
    DATE_ADD('2026-03-15 09:00:00', INTERVAL review_seed.n + MOD(review_seed.n, 7) + review_seed.slot DAY),
    INTERVAL MOD(review_seed.n * 31 + review_seed.slot * 17, 1080) MINUTE
  ) AS `updated_at`
FROM review_seed
WHERE review_seed.slot <= review_seed.review_count;

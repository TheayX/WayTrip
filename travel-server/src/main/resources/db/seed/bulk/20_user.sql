-- 扩容数据脚本：用于向现有库追加用户数据。
-- ============================================================
-- WayTrip 手工扩容用户数据
-- 目标：补充基础用户池，为后续收藏、浏览、评论和订单行为扩容提供用户主体
-- 说明：仅追加新数据，不清空、不覆盖、不修改已有基础数据
-- 依赖：需先执行 db/data.sql
-- 备注：依赖 MySQL 8 递归 CTE；重复执行会因主键或唯一键冲突而失败
-- ============================================================

USE `waytrip_db`;

SET NAMES utf8mb4;

INSERT INTO `user`
  (`id`, `openid`, `nickname`, `phone`, `password`, `avatar_url`, `is_deleted`, `last_login_at`, `created_at`, `updated_at`)
WITH RECURSIVE seq AS (
  SELECT 1 AS n
  UNION ALL
  SELECT n + 1 FROM seq WHERE n < 120
)
SELECT
  10000 + n AS `id`,
  CASE
    WHEN MOD(n, 3) = 0 THEN CONCAT('wx_bulk_', 10000 + n)
    ELSE NULL
  END AS `openid`,
  CONCAT(
    ELT(1 + MOD(n - 1, 12), '林', '陈', '周', '徐', '许', '何', '沈', '宋', '苏', '江', '顾', '程'),
    ELT(1 + MOD(FLOOR((n - 1) / 3), 16), '知远', '清和', '书言', '可心', '沐安', '景川', '星禾', '听澜', '予宁', '初夏', '南乔', '怀青', '云舒', '言蹊', '映雪', '时安')
  ) AS `nickname`,
  CONCAT('139', LPAD(8000000 + n, 8, '0')) AS `phone`,
  CASE
    WHEN MOD(n, 3) = 0 THEN NULL
    ELSE '$2a$10$kNs.tGrq9fm.h/4yF51JUe9DGyC1Jb8nTt9KYsFHBybPvmqBqfoOm'
  END AS `password`,
  '/uploads/avatar/avatar.jpg' AS `avatar_url`,
  0 AS `is_deleted`,
  DATE_ADD('2026-01-01 08:00:00', INTERVAL n DAY) AS `last_login_at`,
  DATE_ADD('2025-09-01 09:00:00', INTERVAL n DAY) AS `created_at`,
  DATE_ADD('2026-01-01 08:00:00', INTERVAL n DAY) AS `updated_at`
FROM seq;

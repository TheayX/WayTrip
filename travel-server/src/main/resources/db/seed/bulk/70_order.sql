-- 扩容数据脚本：用于向现有库追加订单数据。
-- ============================================================
-- WayTrip 手工扩容订单数据
-- 目标：补充订单状态流转样本，支撑订单列表、统计图表和推荐行为权重
-- 说明：仅追加新数据，不清空、不覆盖、不修改已有基础数据
-- 依赖：需先执行 db/data.sql、db/seed/bulk/10_spot.sql、db/seed/bulk/20_user.sql
-- 备注：依赖 MySQL 8 递归 CTE；重复执行会因主键或唯一键冲突而失败
-- ============================================================

USE `waytrip_db`;

SET NAMES utf8mb4;

INSERT INTO `order`
  (`id`, `order_no`, `user_id`, `spot_id`, `quantity`, `total_amount`, `status`, `visit_date`,
   `contact_name`, `contact_phone`, `paid_at`, `cancelled_at`, `refunded_at`, `completed_at`,
   `is_deleted`, `created_at`, `updated_at`)
WITH RECURSIVE user_seq AS (
  SELECT 1 AS n
  UNION ALL
  SELECT n + 1 FROM user_seq WHERE n < 120
),
order_slot AS (
  SELECT 1 AS slot
  UNION ALL
  SELECT slot + 1 FROM order_slot WHERE slot < 2
),
order_seed AS (
  SELECT
    user_seq.n,
    order_slot.slot,
    60000 + (user_seq.n - 1) * 2 + order_slot.slot AS id,
    CONCAT('BULK', DATE_FORMAT(DATE_ADD('2026-03-01 09:00:00', INTERVAL user_seq.n + order_slot.slot DAY), '%Y%m%d'), LPAD((user_seq.n - 1) * 2 + order_slot.slot, 5, '0')) AS order_no,
    10000 + user_seq.n AS user_id,
    CASE
      WHEN order_slot.slot = 1 THEN 1 + MOD(user_seq.n + 4, 16)
      ELSE 1001 + MOD(user_seq.n + 23, 60)
    END AS spot_id,
    1 + MOD(user_seq.n + order_slot.slot, 3) AS quantity,
    CASE MOD(user_seq.n + order_slot.slot, 5)
      WHEN 0 THEN 4
      WHEN 1 THEN 1
      WHEN 2 THEN 0
      WHEN 3 THEN 2
      ELSE 3
    END AS status,
    DATE_ADD('2026-05-20', INTERVAL user_seq.n + order_slot.slot DAY) AS visit_date,
    DATE_ADD('2026-03-01 09:00:00', INTERVAL user_seq.n * 2 + order_slot.slot DAY) AS created_at
  FROM user_seq
  CROSS JOIN order_slot
)
SELECT
  order_seed.id,
  order_seed.order_no,
  order_seed.user_id,
  order_seed.spot_id,
  order_seed.quantity,
  ROUND(COALESCE(spot.price, 0) * order_seed.quantity, 2) AS total_amount,
  order_seed.status,
  order_seed.visit_date,
  user.nickname AS contact_name,
  user.phone AS contact_phone,
  CASE
    WHEN order_seed.status IN (1, 3, 4) THEN DATE_ADD(order_seed.created_at, INTERVAL 15 MINUTE)
    ELSE NULL
  END AS paid_at,
  CASE
    WHEN order_seed.status = 2 THEN DATE_ADD(order_seed.created_at, INTERVAL 1 DAY)
    ELSE NULL
  END AS cancelled_at,
  CASE
    WHEN order_seed.status = 3 THEN DATE_ADD(order_seed.created_at, INTERVAL 3 DAY)
    ELSE NULL
  END AS refunded_at,
  CASE
    WHEN order_seed.status = 4 THEN DATE_ADD(order_seed.visit_date, INTERVAL 12 HOUR)
    ELSE NULL
  END AS completed_at,
  0 AS is_deleted,
  order_seed.created_at,
  CASE
    WHEN order_seed.status = 4 THEN DATE_ADD(order_seed.visit_date, INTERVAL 12 HOUR)
    WHEN order_seed.status = 3 THEN DATE_ADD(order_seed.created_at, INTERVAL 3 DAY)
    WHEN order_seed.status = 2 THEN DATE_ADD(order_seed.created_at, INTERVAL 1 DAY)
    WHEN order_seed.status IN (1, 3, 4) THEN DATE_ADD(order_seed.created_at, INTERVAL 15 MINUTE)
    ELSE order_seed.created_at
  END AS updated_at
FROM order_seed
JOIN `user` user ON user.id = order_seed.user_id
JOIN spot ON spot.id = order_seed.spot_id;

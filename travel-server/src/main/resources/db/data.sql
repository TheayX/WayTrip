-- ============================================================
-- WayTrip 初始化演示数据
-- 特点：先清空业务表，再按模块导入后台、景点、攻略、用户和行为数据。
-- ============================================================
USE waytrip_db;

SET FOREIGN_KEY_CHECKS = 0;

TRUNCATE TABLE `user_spot_view`;
TRUNCATE TABLE `user_spot_review`;
TRUNCATE TABLE `user_spot_favorite`;
TRUNCATE TABLE `user_preference`;
TRUNCATE TABLE `guide_spot_relation`;
TRUNCATE TABLE `spot_image`;
TRUNCATE TABLE `spot_banner`;
TRUNCATE TABLE `order`;
TRUNCATE TABLE `spot`;
TRUNCATE TABLE `spot_category`;
TRUNCATE TABLE `spot_region`;
TRUNCATE TABLE `guide`;
TRUNCATE TABLE `user`;
TRUNCATE TABLE `admin`;

SET FOREIGN_KEY_CHECKS = 1;

-- 1. admin
-- password: admin123
INSERT INTO `admin` (`username`, `password`, `real_name`, `is_enabled`, `is_deleted`, `last_login_at`, `created_at`, `updated_at`)
VALUES
  ('admin', '$2a$10$72UGLb5e/zcaJYQwn.bdPu4c6j/HVujWM5slBk3H0AIH.iLxjmrpS', '超级管理员', 1, 0, '2026-03-03 10:00:00', '2025-01-01 00:00:00', '2026-03-03 10:00:00'),
  ('editor', '$2a$10$72UGLb5e/zcaJYQwn.bdPu4c6j/HVujWM5slBk3H0AIH.iLxjmrpS', '编辑张三', 1, 0, '2026-03-02 14:30:00', '2025-06-01 09:00:00', '2026-03-02 14:30:00'),
  ('operator', '$2a$10$72UGLb5e/zcaJYQwn.bdPu4c6j/HVujWM5slBk3H0AIH.iLxjmrpS', '运营李四', 1, 0, '2026-02-28 16:00:00', '2025-09-01 09:00:00', '2026-02-28 16:00:00')
ON DUPLICATE KEY UPDATE `password` = VALUES(`password`), `real_name` = VALUES(`real_name`);

-- 2. spot_region
INSERT INTO `spot_region` (`parent_id`, `name`, `sort_order`) VALUES
  (0, '北京', 1),
  (0, '上海', 2),
  (0, '广东', 3),
  (0, '浙江', 4),
  (0, '四川', 5),
  (0, '陕西', 6),
  (0, '重庆', 7),
  (0, '江苏', 8),
  (1, '北京市', 1),
  (2, '上海市', 1),
  (3, '广州市', 1),
  (3, '深圳市', 2),
  (4, '杭州市', 1),
  (5, '成都市', 1),
  (6, '西安市', 1),
  (7, '重庆市', 1),
  (8, '南京市', 1),
  (8, '苏州市', 2);

-- 3. spot_category
INSERT INTO `spot_category` (`parent_id`, `name`, `icon_url`, `sort_order`) VALUES
  (0, '自然风光', '/uploads/icons/default.png', 1),
  (0, '历史文化', '/uploads/icons/default.png', 2),
  (0, '主题乐园', '/uploads/icons/default.png', 3),
  (0, '城市观光', '/uploads/icons/default.png', 4),
  (0, '休闲度假', '/uploads/icons/default.png', 5),
  (0, '户外探险', '/uploads/icons/default.png', 6),
  (1, '湖泊', '/uploads/icons/default.png', 1),
  (1, '山岳', '/uploads/icons/default.png', 2),
  (1, '森林公园', '/uploads/icons/default.png', 3),
  (2, '古建筑/宫殿', '/uploads/icons/default.png', 1),
  (2, '博物馆', '/uploads/icons/default.png', 2),
  (2, '古镇古村', '/uploads/icons/default.png', 3),
  (3, '游乐园', '/uploads/icons/default.png', 1),
  (3, '水上乐园', '/uploads/icons/default.png', 2),
  (4, '地标建筑', '/uploads/icons/default.png', 1),
  (4, '特色街区', '/uploads/icons/default.png', 2),
  (5, '温泉度假', '/uploads/icons/default.png', 1),
  (5, '海滨沙滩', '/uploads/icons/default.png', 2),
  (6, '徒步登山', '/uploads/icons/default.png', 1),
  (6, '漂流攀岩', '/uploads/icons/default.png', 2);

-- 4. spot
INSERT INTO `spot`
  (`name`, `description`, `price`, `open_time`, `address`,
   `latitude`, `longitude`, `cover_image_url`,
   `category_id`, `region_id`, `heat_level`, `heat_score`, `avg_rating`, `rating_count`,
   `is_published`)
VALUES
  ('故宫博物院', '明清两代皇宫建筑群，适合历史文化深度游。', 60.00, '08:30-17:00', '北京市东城区景山前街4号',
   39.9163450, 116.3971550, '/uploads/images/default.jpg', 10, 9, 0, 9800, 5.0, 2, 1),
  ('颐和园', '皇家园林代表景区，昆明湖与万寿山景观完整。', 30.00, '06:30-18:00', '北京市海淀区新建宫门路19号',
   39.9993670, 116.2756250, '/uploads/images/default.jpg', 10, 9, 0, 8600, 4.5, 2, 1),
  ('西湖', '杭州城市名片，免费开放，适合慢节奏漫游。', 0.00, '全天开放', '浙江省杭州市西湖区龙井路1号',
   30.2428650, 120.1486810, '/uploads/images/default.jpg', 7, 13, 0, 9600, 4.3, 3, 1),
  ('上海迪士尼乐园', '大型主题乐园，适合亲子和情侣出游。', 475.00, '08:30-20:30', '上海市浦东新区川沙新镇黄赵路310号',
   31.1439040, 121.6694720, '/uploads/images/default.jpg', 13, 10, 0, 9400, 4.5, 2, 1),
  ('兵马俑', '秦始皇兵马俑博物馆，西安代表性历史景点。', 120.00, '08:30-18:00', '陕西省西安市临潼区秦陵北路',
   34.3844310, 109.2783570, '/uploads/images/default.jpg', 11, 15, 0, 8800, 5.0, 2, 1),
  ('成都大熊猫繁育研究基地', '成都热门景区，适合家庭游客和首次到访游客。', 55.00, '07:30-18:00', '四川省成都市成华区外北熊猫大道1375号',
   30.7340000, 104.1458000, '/uploads/images/default.jpg', 7, 14, 0, 8200, 4.5, 2, 1),
  ('外滩', '上海经典滨江景观带，夜景辨识度高。', 0.00, '全天开放', '上海市黄浦区中山东一路',
   31.2400400, 121.4907900, '/uploads/images/default.jpg', 15, 10, 0, 7900, 4.0, 2, 1),
  ('洪崖洞', '重庆热门夜景打卡地，适合夜游和美食体验。', 0.00, '全天开放', '重庆市渝中区嘉陵江滨江路88号',
   29.5630000, 106.5780000, '/uploads/images/default.jpg', 16, 16, 0, 7600, 3.5, 2, 1),
  ('广州塔', '广州地标建筑，可观景、拍照、体验高空项目。', 150.00, '09:30-22:30', '广东省广州市海珠区阅江西路222号',
   23.1085240, 113.3191440, '/uploads/images/default.jpg', 15, 11, 0, 7300, 5.0, 1, 1),
  ('深圳欢乐谷', '深圳热门大型乐园，项目丰富，周末客流较高。', 230.00, '10:00-22:00', '广东省深圳市南山区侨城西街18号',
   22.5391730, 113.9732300, '/uploads/images/default.jpg', 13, 12, 0, 7100, 4.0, 1, 1),
  ('南京夫子庙', '南京传统文化街区，兼具夜游、美食与人文体验。', 0.00, '全天开放', '江苏省南京市秦淮区贡院西街53号',
   32.0228190, 118.7923640, '/uploads/images/default.jpg', 16, 17, 0, 7000, 4.0, 1, 1),
  ('拙政园', '苏州古典园林代表，适合园林审美与慢游。', 80.00, '07:30-17:30', '江苏省苏州市姑苏区东北街178号',
   31.3269070, 120.6313240, '/uploads/images/default.jpg', 10, 18, 0, 7450, 5.0, 1, 1);

-- 5. spot_image
INSERT INTO `spot_image` (`spot_id`, `image_url`, `sort_order`) VALUES
  (1, '/uploads/images/default.jpg', 1),
  (1, '/uploads/images/default.jpg', 2),
  (2, '/uploads/images/default.jpg', 1),
  (2, '/uploads/images/default.jpg', 2),
  (3, '/uploads/images/default.jpg', 1),
  (3, '/uploads/images/default.jpg', 2),
  (4, '/uploads/images/default.jpg', 1),
  (4, '/uploads/images/default.jpg', 2),
  (5, '/uploads/images/default.jpg', 1),
  (5, '/uploads/images/default.jpg', 2),
  (6, '/uploads/images/default.jpg', 1),
  (6, '/uploads/images/default.jpg', 2),
  (7, '/uploads/images/default.jpg', 1),
  (8, '/uploads/images/default.jpg', 1),
  (9, '/uploads/images/default.jpg', 1),
  (10, '/uploads/images/default.jpg', 1),
  (11, '/uploads/images/default.jpg', 1),
  (12, '/uploads/images/default.jpg', 1);

-- 6. spot_banner
INSERT INTO `spot_banner` (`image_url`, `spot_id`, `sort_order`, `is_enabled`) VALUES
  ('/uploads/images/default.jpg', 1, 1, 1),
  ('/uploads/images/default.jpg', 3, 2, 1),
  ('/uploads/images/default.jpg', 4, 3, 1),
  ('/uploads/images/default.jpg', 5, 4, 1),
  ('/uploads/images/default.jpg', 9, 5, 1),
  ('/uploads/images/default.jpg', 12, 6, 1);

-- 7. user
-- web password: 123456
INSERT INTO `user`
  (`openid`, `nickname`, `phone`, `password`, `avatar_url`, `last_login_at`, `created_at`, `updated_at`)
VALUES
  (NULL, '旅行者小明', '13800000001', '$2a$10$kNs.tGrq9fm.h/4yF51JUe9DGyC1Jb8nTt9KYsFHBybPvmqBqfoOm',
   '/uploads/images/avatar.jpg', '2026-03-03 08:00:00', '2025-06-15 10:00:00', '2026-03-03 08:00:00'),
  (NULL, '背包客小红', '13800000002', '$2a$10$kNs.tGrq9fm.h/4yF51JUe9DGyC1Jb8nTt9KYsFHBybPvmqBqfoOm',
   '/uploads/images/avatar.jpg', '2026-03-02 20:00:00', '2025-08-10 14:00:00', '2026-03-02 20:00:00'),
  (NULL, '文艺青年阿杰', '13800000003', '$2a$10$kNs.tGrq9fm.h/4yF51JUe9DGyC1Jb8nTt9KYsFHBybPvmqBqfoOm',
   '/uploads/images/avatar.jpg', '2026-03-01 18:00:00', '2025-09-20 09:00:00', '2026-03-01 18:00:00'),
  ('wx_openid_04', '小程序用户A', '13800000004', NULL,
   '/uploads/images/avatar.jpg', '2026-02-28 12:00:00', '2025-10-05 16:00:00', '2026-02-28 12:00:00'),
  ('wx_openid_05', '小程序用户B', '13800000005', NULL,
   '/uploads/images/avatar.jpg', '2026-02-25 10:00:00', '2025-11-01 08:00:00', '2026-02-25 10:00:00');

-- 8. user_preference
INSERT INTO `user_preference` (`user_id`, `tag`) VALUES
  (1, '历史文化'),
  (1, '城市观光'),
  (2, '户外探险'),
  (2, '自然风光'),
  (3, '历史文化'),
  (3, '自然风光'),
  (4, '主题乐园'),
  (4, '城市观光'),
  (5, '休闲度假');

-- 9. guide
INSERT INTO `guide`
  (`title`, `content`, `cover_image_url`, `category`, `admin_id`, `view_count`, `is_published`)
VALUES
  ('北京经典三日游',
   '<h2>Day 1: 故宫 + 景山</h2><p>建议上午预约故宫，下午登景山俯瞰中轴线。</p><h2>Day 2: 颐和园</h2><p>重点游览长廊、昆明湖和十七孔桥。</p><h2>Day 3: 城市漫步</h2><p>可搭配前门或南锣鼓巷安排轻松行程。</p>',
   '/uploads/images/default.jpg', '行程规划', 1, 2345, 1),
  ('杭州西湖一日游',
   '<h2>推荐路线</h2><p>断桥残雪 -> 白堤 -> 孤山 -> 曲院风荷 -> 苏堤春晓。</p><h2>建议</h2><p>工作日早晨出发，体验更好。</p>',
   '/uploads/images/default.jpg', '行程规划', 1, 1567, 1),
  ('上海亲子乐园攻略',
   '<h2>乐园建议</h2><p>上午优先热门项目，下午安排巡游与演出，晚上看烟花。</p><h2>延伸玩法</h2><p>可搭配外滩夜景作为城市收尾。</p>',
   '/uploads/images/default.jpg', '游玩攻略', 2, 3210, 1),
  ('西安历史文化深度游',
   '<h2>兵马俑</h2><p>建议请讲解或租用讲解器，理解会更完整。</p><h2>市区联动</h2><p>可再安排城墙、钟鼓楼和回民街。</p>',
   '/uploads/images/default.jpg', '行程规划', 1, 1890, 1),
  ('成都休闲慢游',
   '<h2>熊猫基地</h2><p>尽量上午入园，熊猫活动更频繁。</p><h2>城市体验</h2><p>下午可安排宽窄巷子或锦里。</p>',
   '/uploads/images/default.jpg', '行程规划', 3, 1456, 1),
  ('江南园林与夜游路线',
   '<h2>白天园林</h2><p>拙政园建议预留半天，慢慢看建筑与借景。</p><h2>夜游路线</h2><p>晚上可前往南京夫子庙体验秦淮夜景。</p>',
   '/uploads/images/default.jpg', '城市漫游', 2, 1188, 1);

-- 10. guide_spot_relation
INSERT INTO `guide_spot_relation` (`guide_id`, `spot_id`, `sort_order`) VALUES
  (1, 1, 1),
  (1, 2, 2),
  (2, 3, 1),
  (3, 4, 1),
  (3, 7, 2),
  (4, 5, 1),
  (5, 6, 1),
  (6, 12, 1),
  (6, 11, 2);

-- 11. user_spot_favorite
INSERT INTO `user_spot_favorite` (`user_id`, `spot_id`, `created_at`) VALUES
  (1, 1, '2026-01-10 10:00:00'),
  (1, 3, '2026-01-15 14:00:00'),
  (1, 12, '2026-02-21 20:30:00'),
  (2, 3, '2026-01-12 16:00:00'),
  (2, 6, '2026-01-18 11:00:00'),
  (2, 9, '2026-02-22 19:30:00'),
  (3, 1, '2026-01-05 13:00:00'),
  (3, 5, '2026-02-10 15:00:00'),
  (3, 11, '2026-03-01 09:00:00'),
  (4, 4, '2026-01-25 09:30:00'),
  (4, 10, '2026-02-28 18:20:00'),
  (5, 8, '2026-02-08 11:00:00'),
  (5, 7, '2026-02-18 17:45:00');

-- 12. user_spot_view
INSERT INTO `user_spot_view` (`user_id`, `spot_id`, `view_source`, `view_duration`, `created_at`) VALUES
  (1, 1, 'home', 168, '2026-03-01 09:12:00'),
  (1, 3, 'search', 205, '2026-03-01 09:25:00'),
  (1, 12, 'recommend', 186, '2026-03-01 20:10:00'),
  (1, 5, 'guide', 122, '2026-03-02 08:45:00'),
  (1, 9, 'home', 97, '2026-03-02 21:18:00'),
  (1, 11, 'search', 141, '2026-03-04 19:00:00'),
  (2, 3, 'home', 214, '2026-03-01 10:12:00'),
  (2, 6, 'recommend', 244, '2026-03-01 20:41:00'),
  (2, 8, 'search', 131, '2026-03-02 11:20:00'),
  (2, 9, 'home', 160, '2026-03-02 21:35:00'),
  (2, 10, 'recommend', 199, '2026-03-03 19:12:00'),
  (2, 4, 'guide', 178, '2026-03-04 08:55:00'),
  (3, 1, 'home', 156, '2026-03-01 08:20:00'),
  (3, 5, 'search', 223, '2026-03-01 21:20:00'),
  (3, 11, 'recommend', 188, '2026-03-02 19:02:00'),
  (3, 12, 'guide', 209, '2026-03-03 10:18:00'),
  (3, 7, 'home', 111, '2026-03-03 21:55:00'),
  (3, 3, 'search', 146, '2026-03-04 22:02:00'),
  (4, 4, 'home', 312, '2026-03-01 15:00:00'),
  (4, 7, 'guide', 115, '2026-03-01 21:08:00'),
  (4, 10, 'recommend', 246, '2026-03-02 18:50:00'),
  (4, 9, 'search', 151, '2026-03-03 20:21:00'),
  (4, 3, 'home', 104, '2026-03-04 09:33:00'),
  (4, 6, 'recommend', 138, '2026-03-05 07:42:00'),
  (5, 8, 'home', 230, '2026-03-01 22:13:00'),
  (5, 7, 'search', 171, '2026-03-02 08:46:00'),
  (5, 3, 'recommend', 193, '2026-03-02 20:28:00'),
  (5, 9, 'home', 154, '2026-03-03 11:11:00'),
  (5, 11, 'guide', 127, '2026-03-04 19:58:00'),
  (5, 12, 'recommend', 182, '2026-03-05 21:06:00');

-- 13. user_spot_review
INSERT INTO `user_spot_review` (`user_id`, `spot_id`, `score`, `comment`, `created_at`) VALUES
  (1, 1, 5, '故宫很值得慢慢看，建议提前预约并预留半天时间。', '2026-01-20 18:00:00'),
  (1, 3, 5, '西湖适合慢慢逛，春天和傍晚的体验都很好。', '2026-02-01 16:00:00'),
  (1, 12, 5, '园林层次很细腻，适合拍照和安静散步。', '2026-02-20 15:00:00'),
  (2, 2, 4, '颐和园景色很好，建议避开节假日高峰。', '2026-01-28 17:20:00'),
  (2, 3, 4, '骑行环湖体验不错，但热门点位人会比较多。', '2026-01-20 20:00:00'),
  (2, 6, 5, '熊猫太可爱了，早上去能看到更活跃的状态。', '2026-02-05 17:00:00'),
  (2, 9, 5, '夜景很棒，观景平台视野开阔。', '2026-02-26 21:10:00'),
  (3, 1, 5, '第二次来故宫依然有新发现，钟表馆也值得看。', '2026-01-15 15:00:00'),
  (3, 5, 5, '兵马俑现场震撼，历史感非常强。', '2026-02-15 14:00:00'),
  (3, 11, 4, '夫子庙夜游氛围很好，适合边走边吃。', '2026-03-02 21:00:00'),
  (4, 4, 5, '迪士尼氛围很好，烟花秀很值。', '2026-02-01 22:00:00'),
  (4, 7, 4, '外滩夜景很稳，适合第一次来上海的游客。', '2026-02-16 20:00:00'),
  (4, 10, 4, '项目多，排队时间偏长，适合安排整天。', '2026-03-01 20:30:00'),
  (5, 8, 3, '夜景不错，但周末会非常拥挤。', '2026-02-10 21:00:00'),
  (5, 3, 4, '西湖确实很美，工作日去会更舒服。', '2026-02-12 13:00:00'),
  (5, 7, 4, '沿江散步很舒服，拍照很出片。', '2026-02-18 19:20:00'),
  (1, 2, 5, '湖景和长廊都很经典，适合带家人一起去。', '2026-02-24 13:40:00'),
  (3, 12, 5, '园林布局很精致，适合静下来慢慢看。', '2026-03-03 17:30:00'),
  (4, 6, 4, '园区很大，建议早点出发避免后面太赶。', '2026-03-04 16:40:00'),
  (5, 9, 5, '塔上视野很好，晚上城市灯光很漂亮。', '2026-03-05 21:25:00'),
  (2, 8, 4, '楼层错落很有重庆味道，适合夜拍。', '2026-03-06 20:18:00')
ON DUPLICATE KEY UPDATE
  `score` = VALUES(`score`),
  `comment` = VALUES(`comment`),
  `created_at` = VALUES(`created_at`);

-- 14. order
-- status: 0-pending, 1-paid, 2-cancelled, 3-refunded, 4-completed
INSERT INTO `order`
  (`order_no`, `user_id`, `spot_id`, `quantity`, `total_amount`, `status`, `visit_date`,
   `contact_name`, `contact_phone`,
   `paid_at`, `cancelled_at`, `refunded_at`, `completed_at`,
   `created_at`, `updated_at`)
VALUES
  ('ORD20260201000001', 1, 1, 2, 120.00, 4, '2026-02-10',
   '小明', '13800000001',
   '2026-02-01 10:05:00', NULL, NULL, '2026-02-10 18:00:00',
   '2026-02-01 10:00:00', '2026-02-10 18:00:00'),
  ('ORD20260205000001', 2, 6, 2, 110.00, 4, '2026-02-12',
   '小红', '13800000002',
   '2026-02-05 09:15:00', NULL, NULL, '2026-02-12 16:00:00',
   '2026-02-05 09:00:00', '2026-02-12 16:00:00'),
  ('ORD20260210000001', 3, 5, 1, 120.00, 4, '2026-02-18',
   '阿杰', '13800000003',
   '2026-02-10 11:05:00', NULL, NULL, '2026-02-18 17:00:00',
   '2026-02-10 11:00:00', '2026-02-18 17:00:00'),
  ('ORD20260216000001', 5, 12, 2, 160.00, 4, '2026-02-23',
   '用户B', '13800000005',
   '2026-02-16 14:10:00', NULL, NULL, '2026-02-23 18:00:00',
   '2026-02-16 14:00:00', '2026-02-23 18:00:00'),
  ('ORD20260301000001', 1, 3, 2, 0.00, 1, '2026-03-10',
   '小明', '13800000001',
   '2026-03-01 10:05:00', NULL, NULL, NULL,
   '2026-03-01 10:00:00', '2026-03-01 10:05:00'),
  ('ORD20260302000001', 4, 4, 3, 1425.00, 1, '2026-03-20',
   '用户A', '13800000004',
   '2026-03-02 11:15:00', NULL, NULL, NULL,
   '2026-03-02 11:00:00', '2026-03-02 11:15:00'),
  ('ORD20260303000001', 2, 9, 2, 300.00, 1, '2026-03-22',
   '小红', '13800000002',
   '2026-03-03 20:10:00', NULL, NULL, NULL,
   '2026-03-03 20:00:00', '2026-03-03 20:10:00'),
  ('ORD20260304000001', 3, 8, 1, 0.00, 0, '2026-03-18',
   '阿杰', '13800000003',
   NULL, NULL, NULL, NULL,
   '2026-03-04 16:00:00', '2026-03-04 16:00:00'),
  ('ORD20260305000001', 5, 7, 2, 0.00, 0, '2026-03-25',
   '用户B', '13800000005',
   NULL, NULL, NULL, NULL,
   '2026-03-05 08:00:00', '2026-03-05 08:00:00'),
  ('ORD20260306000001', 1, 11, 2, 0.00, 0, '2026-03-28',
   '小明', '13800000001',
   NULL, NULL, NULL, NULL,
   '2026-03-06 19:00:00', '2026-03-06 19:00:00'),
  ('ORD20260220000001', 3, 4, 2, 950.00, 2, '2026-03-01',
   '阿杰', '13800000003',
   NULL, '2026-02-22 10:00:00', NULL, NULL,
   '2026-02-20 16:00:00', '2026-02-22 10:00:00'),
  ('ORD20260222000001', 4, 10, 1, 230.00, 2, '2026-03-08',
   '用户A', '13800000004',
   NULL, '2026-02-24 12:00:00', NULL, NULL,
   '2026-02-22 11:20:00', '2026-02-24 12:00:00'),
  ('ORD20260210000002', 2, 4, 2, 950.00, 3, '2026-02-20',
   '小红', '13800000002',
   '2026-02-10 16:10:00', NULL, '2026-02-12 14:00:00', NULL,
   '2026-02-10 16:00:00', '2026-02-12 14:00:00'),
  ('ORD20260225000001', 5, 1, 3, 180.00, 3, '2026-03-05',
   '用户B', '13800000005',
   '2026-02-25 11:10:00', NULL, '2026-02-28 09:00:00', NULL,
   '2026-02-25 11:00:00', '2026-02-28 09:00:00');

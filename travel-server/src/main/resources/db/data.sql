-- 基础数据初始化脚本
USE waytrip_db;

-- 地区数据
INSERT INTO spot_region (`name`, `sort_order`) VALUES
('北京', 1),
('上海', 2),
('广州', 3),
('深圳', 4),
('杭州', 5),
('成都', 6),
('西安', 7),
('重庆', 8),
('南京', 9),
('苏州', 10);

-- 景点分类数据
INSERT INTO `spot_category` (`parent_id`, `name`, `icon_url`, `sort_order`, `is_deleted`, `created_at`) VALUES
(0, '自然风光', '', 1, 0, NOW()),
(0, '历史文化', '', 2, 0, NOW()),
(0, '主题乐园', '', 3, 0, NOW()),
(0, '城市观光', '', 4, 0, NOW()),
(0, '休闲度假', '', 5, 0, NOW()),
(0, '户外探险', '', 6, 0, NOW());

-- 默认管理员账号（密码: admin123，使用 BCrypt 加密）
-- 密码哈希值是 admin123 的 BCrypt 加密结果
INSERT INTO `admin` (`username`, `password`, `real_name`, `is_enabled`, `is_deleted`)
VALUES ('admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZRGdjGj/n3.rsQ0fEnxRiT1QXqAAK', '系统管理员', 1, 0)
ON DUPLICATE KEY UPDATE
                     `password` = VALUES(`password`),
                     `real_name` = VALUES(`real_name`),
                     `is_enabled` = VALUES(`is_enabled`),
                     `is_deleted` = VALUES(`is_deleted`);

-- 示例景点数据
INSERT INTO `spot`
(`name`, `description`, `price`, `open_time`, `address`, `latitude`, `longitude`,
 `cover_image_url`, `category_id`, `region_id`, `heat_score`, `avg_rating`, `rating_count`,
 `is_published`, `is_deleted`)
VALUES
    ('故宫博物院', '故宫博物院是中国最大的古代文化艺术博物馆，建立于1925年10月10日，位于北京故宫紫禁城内。是在明朝、清朝两代皇宫及其收藏的基础上建立起来的中国综合性博物馆。',
     60.00, '08:30-17:00（16:00停止入场）', '北京市东城区景山前街4号', 39.916345, 116.397155,
     '', 2, 1, 9999, 4.8, 1234, 1, 0),

    ('颐和园', '颐和园是中国清朝时期皇家园林，前身为清漪园，坐落在北京西郊，距城区15公里，占地约290公顷，与圆明园毗邻。',
     30.00, '06:30-18:00', '北京市海淀区新建宫门路19号', 39.999367, 116.275625,
     '', 2, 1, 8888, 4.7, 987, 1, 0),

    ('西湖', '西湖位于浙江省杭州市西湖区龙井路1号，杭州市区西部，景区总面积49平方千米，汇水面积为21.22平方千米，湖面面积为6.38平方千米。',
     0.00, '全天开放', '浙江省杭州市西湖区龙井路1号', 30.242865, 120.148681,
     '', 1, 5, 9500, 4.9, 2345, 1, 0),

    ('上海迪士尼乐园', '上海迪士尼乐园是中国内地首座迪士尼主题乐园，位于上海市浦东新区川沙新镇。',
     475.00, '08:30-20:30', '上海市浦东新区川沙镇黄赵路310号', 31.143904, 121.669472,
     '', 3, 2, 9200, 4.6, 3456, 1, 0),

    ('兵马俑', '秦始皇兵马俑博物馆位于陕西省西安市临潼区，是秦始皇陵的陪葬坑，被誉为\"世界第八大奇迹\"。',
     120.00, '08:30-18:00', '陕西省西安市临潼区秦陵北路', 34.384431, 109.278357,
     '', 2, 7, 8500, 4.8, 1567, 1, 0);

-- 示例攻略数据
INSERT INTO `guide`
(`title`, `content`, `cover_image_url`, `category`, `admin_id`, `view_count`, `is_published`, `is_deleted`)
VALUES
    ('北京三日游攻略',
     '<h2>Day 1: 故宫 + 天安门</h2><p>早上参观天安门广场，然后游览故宫博物院...</p><h2>Day 2: 颐和园 + 圆明园</h2><p>上午游览颐和园，下午参观圆明园遗址...</p><h2>Day 3: 长城</h2><p>全天游览八达岭长城...</p>',
     '', '行程规划', 1, 1234, 1, 0),

    ('杭州西湖一日游',
     '<h2>推荐路线</h2><p>断桥残雪 → 白堤 → 孤山 → 曲院风荷 → 苏堤春晓 → 雷峰塔...</p>',
     '', '行程规划', 1, 567, 1, 0);

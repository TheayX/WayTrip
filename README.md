# WayTrip - 个性化旅游推荐系统

基于协同过滤推荐算法的个性化旅游推荐系统，包含微信小程序（用户端）和 Web 管理后台。

## 技术栈

| 模块     | 技术                                              |
| -------- | ------------------------------------------------- |
| 后端     | Java 17 + Spring Boot 3.2.12 + MyBatis-Plus 3.5.5 |
| 数据库   | MySQL 8.0 + Redis                                 |
| 用户端   | Uni-app + Vue.js 3 + Pinia                        |
| 管理端   | Vue.js 3 + Element Plus + Vite                    |
| API 文档 | Knife4j 4.5.0 (OpenAPI 3)                         |

## 项目结构

```
WayTrip/
├── .kiro/             # 项目文档
├── travel-server/      # 后端服务 (Spring Boot)
├── travel-admin/       # 管理后台 (Vue 3 + Element Plus)
└── travel-miniapp/     # 小程序端 (Uni-app)
```

## 快速开始

### 环境要求

- JDK 17+
- Node.js 18+
- MySQL 8.0+
- Redis 6.0+
- Maven 3.8+

### 1. 数据库初始化

```sql
-- 创建数据库
CREATE DATABASE waytrip_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 导入表结构和初始数据
-- 文件位置: travel-server/src/main/resources/db/schema.sql
-- 文件位置: travel-server/src/main/resources/db/data.sql
```

### 2. 启动后端服务

> **⚠️ 注意**：VS Code 默认可正常启动。若使用 **IntelliJ IDEA**，请务必在启动配置中将 **Working directory** 修改为 `travel-server` 子模块目录（或填入 `$MODULE_WORKING_DIR$`），否则会导致图片无法显示或上传路径错误。

```bash
cd travel-server

# 修改配置文件 (数据库连接、Redis、微信小程序配置等)
# 文件: src/main/resources/application-dev.yml

# 启动服务
mvn spring-boot:run
```

服务启动后访问:

- API 服务: http://localhost:8080
- API 文档: http://localhost:8080/doc.html

### 3. 启动管理后台

```bash
cd travel-admin
npm install

# 启动服务
npm run dev
```

管理后台访问: http://localhost:3000

默认管理员账号: `admin` / `admin123`

### 4. 小程序开发

```bash
cd travel-miniapp
npm install

# 开发编译
npm run dev:mp-weixin

# 打开 微信开发者工具, 导入 dist\dev\mp-weixin 运行
```

## 核心功能

### 用户端 (小程序)

- 微信一键登录
- 景点浏览、搜索、筛选
- 个性化推荐 (ItemCF 协同过滤)
- 旅游攻略阅读
- 景点评分评论
- 收藏功能
- 门票下单 (模拟支付)
- 导航跳转

### 管理端 (Web)

- 景点管理 (增删改查、上下架)
- 攻略管理 (富文本编辑)
- 订单管理 (查看、完成、退款)
- 用户管理 (只读)
- 轮播图管理
- 数据统计仪表板

## API 接口

- 用户端 API: `/api/v1/*`
- 管理端 API: `/api/admin/v1/*`

详细接口文档请查看: http://localhost:8080/doc.html

## 数据口径规则（PR-5）

为确保列表展示、详情查询与仪表盘统计一致，系统统一采用以下规则：

- 软删除规则：业务查询默认过滤 `is_deleted = 0`。
- 发布规则：用户端可见内容默认过滤 `is_published = 1`（如景点、攻略及其关联展示）。
- 时间戳规则：
  - `created_at` 表示创建时间，仅首次写入；
  - `updated_at` 表示更新时间，更新时自动维护；
  - 对不含 `updated_at` 的表，不做更新时间写入要求。
- 分类规则：`/api/v1/spots/filters` 同时返回扁平 `categories` 与树形 `categoryTree`，兼容旧客户端并支持新端分类树渲染。

详细说明见：[接口文档-数据口径说明](./.kiro/specs/travel-recommendation-system/api.md)

## 推荐算法

采用 ItemCF (基于物品的协同过滤) 算法:

- 基于用户评分计算物品相似度
- 冷启动策略: 热门推荐 + 偏好标签引导
- 推荐过滤: 排除已评分/已收藏/已下单景点

## 文档

- [需求文档](./.kiro/specs/travel-recommendation-system/requirements.md)
- [设计文档](./.kiro/specs/travel-recommendation-system/design.md)
- [API 文档](./.kiro/specs/travel-recommendation-system/api.md)
- [数据库设计](./.kiro/specs/travel-recommendation-system/database.md)

## License

MIT

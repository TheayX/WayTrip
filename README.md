# WayTrip - 个性化旅游推荐系统

基于协同过滤推荐算法的个性化旅游推荐系统，包含微信小程序（用户端）、Web 用户端和 Web 管理后台，后端统一提供 RESTful API。
> 小程序端与 Web 用户端可独立运行（仅使用 Web 或仅使用小程序均可正常体验）

## 技术栈

| 模块       | 技术                                              |
| ---------- | ------------------------------------------------- |
| 后端       | Java 17 + Spring Boot 3.2.12 + MyBatis-Plus 3.5.5 |
| 数据库     | MySQL 8.0 + Redis                                 |
| 小程序端   | Uni-app + Vue.js 3 + Pinia                        |
| Web 用户端 | Vue.js 3 + Element Plus + Pinia + Vite            |
| 管理端     | Vue.js 3 + Element Plus + ECharts + Vite          |
| API 文档   | Knife4j 4.5.0 (OpenAPI 3)                         |

## 项目结构

```
WayTrip/
├── .kiro/              # 项目文档（需求、设计、API、数据库、任务）
├── travel-admin/       # 管理后台 (Vue 3 + Element Plus)
├── travel-miniapp/     # 小程序端 (Uni-app)
├── travel-server/      # 后端服务 (Spring Boot)
└── travel-web/         # Web 用户端 (Vue 3 + Element Plus)
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

> **注意**：VS Code 默认可正常启动。若使用 **IntelliJ IDEA**，请务必在启动配置中将 **Working directory** 修改为 `travel-server` 子模块目录（或填入 `$MODULE_WORKING_DIR$`），否则会导致图片无法显示或上传路径错误。
>
> **配置提示**：建议本地开发将 [active](travel-server/src/main/resources/application.yml) 切换到 `dev` 配置，并在 `application-dev.yml` 中填写自己的数据库、Redis、JWT、微信小程序等参数；如果使用 `prod` 配置，请在 `travel-server` 根目录创建 `.env` 文件并填写对应参数（参考 `.env.example`）。

```bash
cd travel-server

# 方式一：dev 环境 — 直接修改 application-dev.yml
# 方式二：prod 环境 — 复制 .env.example 为 .env 并填写配置
cp .env.example .env
# 编辑 .env 填入数据库密码、Redis密码、JWT密钥、微信配置等

# 启动服务
mvn spring-boot:run
```

服务启动后访问:

- API 服务: http://localhost:8080
- API 文档: http://localhost:8080/doc.html

### 3. 启动 Web 用户端

```bash
cd travel-web
npm install

# 启动服务
npm run dev
```

Web 用户端访问: http://localhost:3001

### 4. 启动管理后台

```bash
cd travel-admin
npm install

# 启动服务
npm run dev
```

管理后台访问: http://localhost:3000

默认管理员账号: `admin` / `admin123`

### 5. 小程序开发

```bash
cd travel-miniapp
npm install

# 开发编译
npm run dev:mp-weixin

# 打开微信开发者工具，导入 dist/dev/mp-weixin 运行
```

## 核心功能

### 用户端（小程序 + Web）

- 用户认证（小程序微信一键登录 / Web 端手机号注册登录）
- 景点浏览、搜索、筛选（地区 / 分类 / 热度 / 评分 / 价格排序）
- 个性化推荐（ItemCF 协同过滤）
- 旅游攻略阅读（富文本 + 关联景点）
- 景点评分评论
- 收藏管理
- 门票下单（模拟支付）
- 地图导航跳转（小程序端）
- 个人中心（修改昵称 / 头像上传 / 偏好标签 / 修改密码 / 注销账户）

### 管理端（Web）

- 景点管理（增删改查、上下架、图片上传）
- 攻略管理（富文本编辑、关联景点）
- 订单管理（查看、完成、退款）
- 用户管理（只读，脱敏展示）
- 管理员管理（创建、编辑、重置密码、删除）
- 轮播图管理（排序、启用 / 禁用）
- 数据统计仪表板（概览卡片、订单趋势图、热门景点排行）

## API 接口

| 端           | 前缀              | 说明           |
| ------------ | ----------------- | -------------- |
| 用户端       | `/api/v1/*`       | 小程序 & Web 共用 |
| 管理端       | `/api/admin/v1/*` | 管理后台专用   |

详细接口文档请查看: http://localhost:8080/doc.html

## 推荐算法

采用 ItemCF（基于物品的协同过滤）算法：

- 基于用户评分矩阵计算物品余弦相似度，结果缓存至 Redis
- 冷启动策略：评分不足 3 条时返回热门/最新景点 + 偏好标签引导
- 推荐过滤：排除已评分 / 已收藏 / 已下单（不含已取消）景点
- 定时任务自动更新相似度矩阵

## 文档

- [需求文档](./.kiro/specs/travel-recommendation-system/requirements.md)
- [设计文档](./.kiro/specs/travel-recommendation-system/design.md)
- [API 文档](./.kiro/specs/travel-recommendation-system/api.md)
- [数据库设计](./.kiro/specs/travel-recommendation-system/database.md)
- [开发任务](./.kiro/specs/travel-recommendation-system/tasks.md)

## License

MIT

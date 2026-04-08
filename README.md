# WayTrip - 个性化旅游推荐系统

WayTrip 是一个面向旅游场景的个性化推荐系统，当前包含 4 个可独立运行的子项目：

- `travel-miniapp`：微信小程序用户端
- `travel-web`：Web 用户端
- `travel-admin`：Web 管理后台
- `travel-server`：统一后端服务
> 小程序端与 Web 用户端可独立运行（仅使用 Web 或仅使用小程序均可正常体验）

**🤖分支说明：项目另有 `ai-chatbot` 分支，用于 AI 客服相关实验与实现，与 `main` 主线分支隔离维护。**

## 技术栈

| 模块 | 技术 |
| --- | --- |
| 后端 | Java 17 + Spring Boot 3.5.11 + MyBatis-Plus 3.5.5 |
| 数据库 | MySQL 8.0 + Redis |
| 小程序端 | Uni-app + Vue 3 + Pinia |
| Web 用户端 | Vue 3 + Vite + Element Plus + Pinia |
| 管理端 | Vue 3 + Vite + Element Plus + Pinia + ECharts |
| API 文档 | SpringDoc（OpenAPI 3） |

## 项目结构

```text
WayTrip/
├── .kiro/              # 项目文档（需求、设计、API、数据库、任务）
├── travel-admin/       # 管理后台
├── travel-miniapp/     # 小程序端
├── travel-server/      # 后端服务
└── travel-web/         # Web 用户端
```

## 功能概览

### 用户端（小程序 + Web）

- 用户认证与资料管理
- 首页轮播图、热门推荐、个性化推荐
- 发现页聚合浏览
- 附近景点探索
- 景点列表、搜索、筛选、详情
- 攻略列表、攻略详情、关联景点跳转
- 评分评论、收藏、浏览足迹
- 门票订单创建、支付、取消、查看
- 个人中心、我的互动、设置、偏好标签

### 管理端

- 仪表板与运营概览
- 景点管理、热度同步、评分回刷
- 攻略管理与关联景点维护
- 订单流转管理
- 用户管理、管理员管理
- 地区管理、分类管理、轮播图管理
- 推荐总览、推荐配置、推荐调试预览、相似邻居预览

### 后端推荐系统

- 基于 ItemCF 的个性化推荐
- 融合评分、收藏、订单、浏览行为
- 浏览行为支持来源与停留时长加权
- 冷启动兜底与偏好引导
- 热度重排与结果过滤
- Redis 存储推荐配置、运行状态、用户推荐缓存和相似度矩阵

## 快速开始

### 环境要求

- JDK 17+
- Maven 3.8+
- Node.js 18+
- MySQL 8.0+
- Redis 6.0+

### 1. 初始化数据库

```sql
CREATE DATABASE waytrip_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

导入以下脚本：

- `travel-server/src/main/resources/db/schema.sql`
- `travel-server/src/main/resources/db/data.sql`

### 2. 启动后端服务

开发环境建议：

- 后端环境变量使用 `.env.example -> .env` 这套约定
- 如果 `travel-server/src/main/resources/application.yml` 使用 `prod`，则在 `travel-server` 下创建 `.env`，参考 `travel-server/.env.example`
- 如果切到 `dev`，则直接在 `travel-server/src/main/resources/application-dev.yml` 中填写本地数据库、Redis、JWT、微信与上传目录配置
- `UPLOAD_PATH` 建议优先使用绝对路径；如果继续使用相对路径，请固定启动方式

```bash
cd travel-server

# 启动服务
mvn spring-boot:run
```

启动后可访问：

- API 服务：`http://localhost:8080`
- API 文档界面：`http://localhost:8080/swagger-ui/index.html`
- API 文档数据：`http://localhost:8080/v3/api-docs`

### 3. 启动 Web 用户端

环境变量约定：

- 前端环境变量使用 `.env.example -> .env.local` 这套约定
- 默认情况下不复制环境文件也能直接启动
- 只有在 HTTPS 反代、ngrok 或特殊代理联调时，才需要按各子项目 README 复制并修改 `.env.local`

```bash
cd travel-web
npm install

# 启动服务
npm run dev
```

默认访问：`http://localhost:3001`

### 4. 启动管理后台

```bash
cd travel-admin
npm install

# 启动服务
npm run dev
```

默认访问：`http://localhost:3000`

默认管理员账号：`admin / admin123`

### 5. 启动微信小程序

```bash
cd travel-miniapp
npm install

# 开发编译
npm run dev:mp-weixin

# 打开微信开发者工具，导入 dist/dev/mp-weixin 运行
```

补充说明：

- 小程序端默认也可以直接联调本地后端
- 如果需要通过 HTTPS 反代消除开发工具里的 HTTP 警告，再复制 `travel-miniapp/.env.example` 为 `.env.local` 并按需修改

## 接口约定

| 端 | 前缀 | 说明 |
| --- | --- | --- |
| 用户端 | `/api/v1/*` | 小程序端与 Web 用户端共用 |
| 管理端 | `/api/admin/v1/*` | 管理后台专用 |

用户资料接口约定：

- 主入口统一使用 `/api/v1/user/*`
- `/api/v1/auth/*` 仅保留登录注册和兼容能力

## 文档

- [需求文档](./.kiro/specs/travel-recommendation-system/requirements.md)
- [设计文档](./.kiro/specs/travel-recommendation-system/design.md)
- [API 文档](./.kiro/specs/travel-recommendation-system/api.md)
- [数据库设计](./.kiro/specs/travel-recommendation-system/database.md)
- [开发任务](./.kiro/specs/travel-recommendation-system/tasks.md)

## License

MIT

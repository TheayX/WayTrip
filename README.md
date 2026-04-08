# WayTrip - 个性化旅游推荐系统

WayTrip 是一个面向旅游场景的个性化推荐系统，当前由 4 个可独立运行的子项目组成：

- `travel-admin`：Web 管理后台
- `travel-miniapp`：微信小程序用户端
- `travel-server`：统一后端服务
- `travel-web`：Web 用户端
> 小程序端与 Web 用户端可独立运行（仅使用 Web 或仅使用小程序均可正常体验）

**🤖分支说明：项目另有 `ai-chatbot` 分支，用于 AI 客服相关实验与实现，与 `main` 主线分支隔离维护。**

## 技术栈

| 模块 | 技术 |
| --- | --- |
| 后端 | Java 17 + Spring Boot 3.5.11 + MyBatis-Plus 3.5.5 |
| 缓存与中间件 | Redis |
| 数据库 | MySQL 8.0 |
| 小程序端 | Uni-app + Vue 3 + Pinia |
| Web 用户端 | Vue 3 + Vite 5 + Element Plus + Pinia |
| 管理端 | Vue 3 + Vite 5 + Element Plus + Pinia + ECharts |
| API 文档 | SpringDoc / OpenAPI 3 |

## 仓库结构

```text
WayTrip/
├─ docs/                            项目文档与规格说明
├─ travel-admin/                    管理后台
├─ travel-miniapp/                  微信小程序端
├─ travel-server/                   后端服务
├─ travel-web/                      Web 用户端
└─ README.md
```

## 当前能力

### 用户端

- 用户认证、资料维护、偏好设置、密码修改、账户注销
- 首页轮播图、热门推荐、个性化推荐、冷启动偏好引导
- 发现页聚合浏览、搜索、附近探索、更多玩法入口
- 景点列表、筛选、详情、相似景点、浏览足迹
- 攻略列表、详情、分类、关联景点
- 评分评论、收藏、我的互动
- 订单创建、支付、取消、列表与详情

### 管理端

- 仪表板与运营概览
- 景点、攻略、轮播图、地区、分类维护
- 订单流转管理
- 用户管理、管理员管理、用户洞察
- 推荐总览、推荐配置、运行状态、调试预览

### 后端

- 用户端与管理端统一 REST API
- JWT 鉴权与角色隔离
- 文件上传与静态资源访问
- 基于 ItemCF 的景点推荐
- Redis 推荐缓存、状态缓存、相似度矩阵与热度去重

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

导入脚本：

- [schema.sql](./travel-server/src/main/resources/db/schema.sql)
- [data.sql](./travel-server/src/main/resources/db/data.sql)

### 2. 启动后端

后端环境变量约定：

- 使用 `travel-server/.env.example -> travel-server/.env`
- 本地开发可直接按模板填写数据库、Redis、JWT、微信与上传目录配置
- 默认 [profile](./travel-server/src/main/resources/application.yml) 为 `prod`
- 如果继续使用 `prod`，则在 `travel-server` 下创建 `.env`，参考 `travel-server/.env.example`
- 如果切到 `dev`，则直接在 `travel-server/src/main/resources/application-dev.yml` 中填写本地数据库、Redis、JWT、微信与上传目录配置
- `UPLOAD_PATH` 建议优先使用绝对路径；如果继续使用相对路径，请固定启动方式

```bash
cd travel-server

# 启动服务
mvn spring-boot:run
```

默认地址：

- API 服务：`http://localhost:8080`
- API 文档界面：`http://localhost:8080/swagger-ui/index.html`
- API 文档数据：`http://localhost:8080/v3/api-docs`

### 3. 启动 Web 用户端

前端环境变量约定：

- `travel-web`、`travel-admin`、`travel-miniapp` 统一使用 `.env.example -> .env.local`
- 默认情况下不复制 `.env.local` 也能直接联调本地后端
- 只有在 HTTPS 反代、ngrok 或特殊代理联调时，才需要覆盖本地环境变量

```bash
cd travel-web
npm install

# 启动服务
npm run dev
```

默认地址：`http://localhost:3001`

### 4. 启动管理端

```bash
cd travel-admin
npm install

# 启动服务
npm run dev
```

默认地址：`http://localhost:3000`

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

- 小程序默认可直接联调本地 `http://localhost:8080`
- 如果需要通过 HTTPS 反代消除开发工具中的 HTTP 警告，可参考 [docs/dev-https-proxy.md](./docs/dev-https-proxy.md)

## 接口前缀

| 端 | 前缀 | 说明 |
| --- | --- | --- |
| 用户端 | `/api/v1/*` | 小程序端与 Web 用户端共用 |
| 管理端 | `/api/admin/v1/*` | 管理后台专用 |

用户资料接口约定：

- 主入口统一使用 `/api/v1/user/*`
- `/api/v1/auth/*` 仅保留登录注册和兼容能力

## 文档

- [文档索引](./docs/README.md)
- [需求文档](./docs/specs/travel-recommendation-system/requirements.md)
- [设计文档](./docs/specs/travel-recommendation-system/design.md)
- [API 文档](./docs/specs/travel-recommendation-system/api.md)
- [数据库文档](./docs/specs/travel-recommendation-system/database.md)
- [任务文档](./docs/specs/travel-recommendation-system/tasks.md)

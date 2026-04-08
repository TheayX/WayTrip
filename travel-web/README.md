# travel-web

WayTrip Web 用户端，面向 PC 浏览场景，当前采用 `app / shared / modules` 三层结构组织代码。

## 技术栈

- Vue 3
- Vite 5
- Vue Router 4
- Pinia
- Element Plus
- Axios
- SCSS

## 启动与构建

安装依赖：

```bash
npm install
```

本地开发：

```bash
npm run dev
```

生产构建：

```bash
npm run build
```

本地预览：

```bash
npm run preview
```

默认开发地址：`http://localhost:3001`

## 环境变量

- 默认不创建 `.env.local` 也能直接开发
- 需要本机 HTTPS 反代、ngrok 或特殊代理联调时，再复制 `.env.example` 为 `.env.local`
- 当前可选变量：
  - `VITE_DEV_PROXY_TARGET`
  - `VITE_DEV_HOST`
  - `VITE_DEV_ALLOWED_HOSTS`

## 当前结构

```text
src/
├─ app/                             应用壳层
├─ shared/                          共享层
└─ modules/                         业务模块层
   ├─ account/
   ├─ auth/
   ├─ budget-travel/
   ├─ discover/
   ├─ favorite/
   ├─ guide/
   ├─ home/
   ├─ more/
   ├─ nearby/
   ├─ order/
   ├─ random-pick/
   ├─ recommendation/
   ├─ review/
   ├─ search/
   ├─ spot/
   ├─ traveler-reviews/
   └─ trending-views/
```

## 主要能力

- 首页、发现页、搜索页、附近页
- 景点列表、详情、相似景点、浏览足迹回写
- 攻略列表、详情、关联景点
- 推荐页、冷启动偏好引导、随心一选、穷游玩法、游客口碑、近期热看
- 登录注册、个人资料、设置、我的互动、收藏、订单

## 开发约定

- 默认继续沿用 `app / shared / modules` 三层结构
- 业务代码按模块聚合，不回退到旧的全局平铺目录
- 新增注释统一使用中文，只在关键逻辑和易误解分支补充说明
- 页面详情跳转统一复用 `src/shared/constants/spot-detail.js`

## 构建说明

当前已对以下依赖做基础拆包：

- `element-plus`
- `@element-plus/icons-vue`
- `vue-router / pinia / vue`
- `axios`

如果构建时仍看到大 chunk 警告，属于当前依赖体积带来的已知现象，不代表构建失败。

## 相关文档

- [根说明](../README.md)
- [开发期 HTTPS 与反代说明](../docs/dev-https-proxy.md)
- [设计文档](../docs/specs/travel-recommendation-system/design.md)

# travel-admin

WayTrip 管理后台，面向运营和内容维护场景，当前采用 `app / shared / modules` 三层结构组织代码。

## 技术栈

- Vue 3
- Vite 5
- Vue Router 4
- Pinia
- Element Plus
- ECharts
- WangEditor
- Axios

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

默认开发地址：`http://localhost:3000`

## 环境变量

- 默认不创建 `.env.local` 也能直接开发
- 需要本机 HTTPS 反代、ngrok 或特殊代理联调时，再复制 `.env.example` 为 `.env.local`
- 当前可选变量：
  - `VITE_DEV_PROXY_TARGET`
  - `VITE_API_ORIGIN`
  - `VITE_DEV_HOST`
  - `VITE_DEV_ALLOWED_HOSTS`

## 当前结构

```text
src/
├─ app/                             应用壳层
├─ shared/                          共享层
└─ modules/                         业务模块层
   ├─ banner/
   ├─ category/
   ├─ guide/
   ├─ order/
   ├─ overview/
   ├─ recommendation/
   ├─ region/
   ├─ spot/
   ├─ system/
   └─ user-ops/
```

## 主要能力

- 管理员登录与鉴权
- 仪表板与运营概览
- 景点、攻略、轮播图、地区、分类维护
- 订单流转管理
- 用户管理、管理员管理、用户偏好/收藏/浏览洞察
- 推荐总览、配置调整、运行状态、用户推荐预览、相似邻居预览

## 开发约定

- 业务模块内部按需拆分 `pages / components / api / composables`
- 不回退到旧的全局 `views / api / utils / styles` 平铺结构
- 新增注释统一使用中文，只写关键原因，不做翻译代码式注释
- 模块结构优先服务维护成本，不为了“看起来规范”创建空目录

## 相关文档

- [根说明](../README.md)
- [设计文档](../docs/specs/travel-recommendation-system/design.md)
- [API 文档](../docs/specs/travel-recommendation-system/api.md)

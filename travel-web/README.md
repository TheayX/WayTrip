# travel-web

WayTrip Web 用户端，面向 PC 浏览场景，当前采用 `app / shared / modules` 三层结构组织代码。

## 技术栈

- Vue 3
- Vite 5
- Vue Router 4
- Pinia
- pinia-plugin-persistedstate
- Element Plus
- Axios
- SCSS
- ESLint 9

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

代码检查：

```bash
npm run lint
npm run lint:fix
```

默认开发地址：`http://localhost:3001`

## 环境变量

- 默认不创建 `.env.local` 也能直接开发
- 需要 HTTPS 反代、ngrok 或特殊代理联调时，再复制 `.env.example` 为 `.env.local`
- 当前可选变量：
  - `VITE_DEV_PROXY_TARGET`：本地开发代理目标，默认 `http://localhost:8080`
  - `VITE_DEV_HOST`：开发服务器绑定 Host
  - `VITE_DEV_ALLOWED_HOSTS`：额外允许访问的 Host 列表，多个值用英文逗号分隔

## 目录结构

```text
travel-web/
├─ .env.example                     环境变量模板，定义代理目标与额外 Host 白名单
├─ .env.local                       本机私有联调配置，由开发者自行创建，不提交 Git
├─ .gitignore                       Git 忽略规则
├─ eslint.config.mjs                ESLint Flat Config
├─ index.html                       Vite HTML 入口，定义页面挂载点与基础文档信息
├─ package.json                     依赖与 npm scripts 入口
├─ package-lock.json                npm 依赖锁文件
├─ vite.config.js                   Vite 配置，负责别名、代理、拆包和 3001 端口
├─ public/
│  └─ waytrip-standard-mark.svg     静态公共资源，构建时原样拷贝
└─ src/
  ├─ App.vue                       Web 根组件，承接全局布局和路由出口
  ├─ main.js                       应用启动入口，注册路由、Pinia、Element Plus 与全局样式
  ├─ app/                          应用壳层
  │  ├─ layouts/                   顶层布局，如主壳布局、认证布局、账户中心布局
  │  └─ router/                    路由实例、路由表与导航守卫
  ├─ shared/                       共享层，放跨模块复用的基础能力与资源
  │  ├─ api/
  │  │  └─ client.js               Axios 请求封装，统一接口访问与错误处理
  │  ├─ assets/
  │  │  ├─ brand/                  品牌资源
  │  │  └─ tabbar/                 默认头像、空态图等通用图片资源
  │  ├─ constants/                 全局常量，如路由名、路由路径、搜索配置、详情跳转
  │  ├─ lib/                       通用工具，如浏览足迹、定位、冷启动引导
  │  ├─ styles/
  │  │  └─ index.scss              全局样式总入口
  │  └─ ui/                        跨模块复用界面组件
  └─ modules/                      业务模块层
     ├─ account/                   账户中心，含页面、局部组件、常量与用户状态
     ├─ auth/                      登录注册
     ├─ budget-travel/             穷游玩法
     ├─ discover/                  发现页
     ├─ favorite/                  收藏
     ├─ guide/                     攻略
     ├─ home/                      首页
     ├─ more/                      更多玩法
     ├─ nearby/                    附近景点
     ├─ order/                     订单
     ├─ random-pick/               随心一选
     ├─ recommendation/            推荐页
     ├─ review/                    评分评论
     ├─ search/                    搜索
     ├─ spot/                      景点
     ├─ traveler-reviews/          游客口碑
     └─ trending-views/            近期热看
```

## 模块组织方式

模块内部按复杂度按需拆分，不强制所有目录都出现：

```text
light-module/
├─ api.js
└─ index.vue

feature-module/
├─ api.js 或 api/
├─ components/
├─ composables/                     可选，仅在模块内存在复用逻辑时创建
├─ pages/                           可选，存在多页面或页面级拆分时使用
├─ store/                           可选，存在模块私有状态时使用
├─ constants/                       可选，存在模块私有常量时使用
└─ styles/                          可选
```

当前项目中的典型例子：

- `discover / nearby / search / favorite / review / more / traveler-reviews / trending-views / budget-travel / random-pick` 以单页模块为主
- `spot / guide / order / auth / account` 属于复杂模块，按页面、组件、状态或常量进一步拆分
- `account/store/user.js` 为当前实际用户状态入口，不在 `app` 下单独维护全局 store 目录

## 主要能力

- 首页、发现页、推荐页、搜索页、附近页
- 景点列表、详情、相似景点、浏览足迹回写
- 攻略列表、详情、关联景点
- 登录注册、个人资料、设置、我的互动、收藏、订单
- 随心一选、穷游玩法、游客口碑、近期热看

## 开发约定

- 默认沿用 `app / shared / modules` 三层结构
- 业务代码按模块聚合，不回退到旧的全局平铺目录
- 顶层布局统一放 `app/layouts`，业务状态优先收敛到所属模块内部
- 新增注释统一使用中文，只在关键逻辑和易误解分支补充说明
- 页面详情跳转统一复用 `src/shared/constants/spot-detail.js`

## 构建说明

当前已对以下依赖做基础拆包：

- `element-plus`
- `@element-plus/icons-vue`
- `vue-router / pinia / vue`
- `axios`

如果构建时仍出现大 chunk 警告，属于当前依赖体积带来的已知现象，不代表构建失败。

## 文档后续可补充

- 路由清单：补充各页面路径、布局归属与是否要求登录
- 账户链路说明：补充登录态持久化、账户中心布局与用户状态流转
- 共享组件说明：补充 `shared/ui` 组件用途与适用场景
- 搜索与详情跳转说明：补充搜索配置、详情跳转和足迹记录之间的关系

## 相关文档

- [仓库总览](../README.md)
- [开发期 HTTPS 与反代说明](../docs/dev-https-proxy.md)
- [设计文档](../docs/specs/travel-recommendation-system/design.md)

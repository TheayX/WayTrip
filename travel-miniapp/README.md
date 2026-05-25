# travel-miniapp

WayTrip 微信小程序端，基于 Uni-app 构建，当前按“页面业务域 + 公共能力”组织代码。

## 技术栈

- Uni-app
- Vue 3
- Pinia
- Sass
- ESLint 9

## 启动与构建

安装依赖：

```bash
npm install
```

微信小程序开发编译：

```bash
npm run dev:mp-weixin
```

微信小程序构建：

```bash
npm run build:mp-weixin
```

代码检查：

```bash
npm run lint
npm run lint:fix
```

再使用微信开发者工具导入 `dist/dev/mp-weixin` 或 `dist/build/mp-weixin`。

## 环境变量

- 默认不创建 `.env.local` 也能直接联调本地后端
- 需要通过 HTTPS 反代消除微信开发者工具里的 HTTP 警告时，再复制 `.env.example` 为 `.env.local`
- 当前可选变量：
  - `VITE_API_ORIGIN`：接口源站地址，默认可指向本地后端 `http://localhost:8080`

## 目录结构

```text
travel-miniapp/
├─ .env.example                     环境变量模板，定义小程序接口源站地址
├─ .env.local                       本机私有联调配置，由开发者自行创建，不提交 Git
├─ .gitignore                       Git 忽略规则
├─ eslint.config.mjs                ESLint Flat Config
├─ package.json                     依赖与 npm scripts 入口
├─ package-lock.json                npm 依赖锁文件
├─ vite.config.js                   Uni-app 的 Vite 配置，挂载插件并配置 Sass 编译
└─ src/
  ├─ App.vue                       小程序根组件，承接全局生命周期
  ├─ main.js                       应用启动入口，注册 Pinia 与全局能力
  ├─ index.html                    H5 容器模板，主要服务 Uni-app 的 H5 构建场景
  ├─ manifest.json                 Uni-app 平台配置，如 appid、图标与发行参数
  ├─ pages.json                    页面路由、导航栏、tabBar 与 easycom 配置中心
  ├─ uni.scss                      全局 Sass 变量与样式入口
  ├─ api/                          按业务域拆分的接口请求，如 auth、spot、guide、order
  ├─ components/                   跨页面公共组件
  │  ├─ feature-entry/             功能入口类组件
  │  └─ PreferenceCategorySelector.vue
  ├─ composables/
  │  └─ useRecommendationFeed.js   推荐流复用逻辑
  ├─ constants/
  │  └─ feature-entry-registry.js  功能入口注册表，统一维护“更多玩法”等入口元数据
  ├─ pages/                        页面业务域
  │  ├─ index/                     首页，含首页局部组件
  │  ├─ discover/                  发现页
  │  ├─ recommendation/            个性推荐页
  │  ├─ spot/                      景点列表、详情、搜索、附近景点
  │  ├─ guide/                     攻略列表与详情
  │  ├─ order/                     订单列表、详情、创建订单
  │  ├─ mine/                      我的主页、资料、设置、互动与账号操作
  │  ├─ more/                      更多玩法入口页
  │  ├─ random-pick/               随心一选
  │  ├─ budget-travel/             穷游玩法
  │  ├─ traveler-reviews/          游客口碑
  │  └─ trending-views/            近期热看
  ├─ services/                     玩法页或聚合页的数据编排逻辑
  ├─ static/                       静态资源，如品牌图、tabBar 图标、地图标记、默认占位图
  ├─ stores/
  │  └─ user.js                    用户全局状态，管理登录态与资料信息
  └─ utils/                        通用工具，如请求封装、鉴权、定位、导航与展示辅助
```

## 页面组织方式

页面目录按业务域组织，不按组件类型平铺。页面内部按复杂度按需拆分：

```text
light-page/
└─ index.vue

feature-page/
├─ index.vue 或 list/detail.vue
├─ components/                      可选，仅在页面内部存在复用组件时创建
├─ composables/                     可选，仅在页面内部存在复用逻辑时创建
└─ services/                        可选，仅在页面内部存在局部数据编排时创建
```

当前项目中的典型例子：

- `discover / recommendation / more / random-pick / budget-travel / traveler-reviews / trending-views` 以单页为主
- `spot / guide / order / mine` 属于多页面业务域，按 `list / detail / settings` 等子页面进一步拆分
- `pages/index/components` 属于首页私有组件，不放入全局 `components`

## 主要能力

- 微信登录、手机号绑定、资料与偏好管理
- 首页、发现页、个性推荐页
- 景点列表、详情、搜索、附近景点、地图导航、浏览足迹
- 攻略列表、详情、关联景点
- 收藏、评分评论、我的互动
- 订单创建、支付、取消、订单列表与详情
- 随心一选、穷游玩法、游客口碑、近期热看

## 开发约定

- 页面按业务域组织，不按展示位置组织
- 接口请求、聚合逻辑、页面渲染分层维护
- 跨页面复用组件放 `src/components`，页面私有组件放对应页面目录内部
- 新增注释统一使用中文，只在关键逻辑补充说明
- 景点详情跳转统一复用 `src/utils/spot-detail.js`

## 文档后续可补充

- 页面路由清单：补充 `pages.json` 中每个页面路径与用途的对应关系
- 接口域说明：补充 `src/api` 下各文件与后端模块的映射关系
- 登录链路说明：补充登录、手机号绑定、token 持久化的交互流程
- 开发者工具联调说明：补充 HTTPS、合法域名、定位权限等常见问题

## 相关文档

- [仓库总览](../README.md)
- [开发期 HTTPS 与反代说明](../docs/dev-https-proxy.md)
- [需求文档](../docs/specs/travel-recommendation-system/requirements.md)

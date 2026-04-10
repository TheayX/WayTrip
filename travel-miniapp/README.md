# travel-miniapp

WayTrip 微信小程序端，基于 Uni-app 构建，当前按“页面业务域 + 公共能力”组织代码。

## 技术栈

- Uni-app
- Vue 3
- Pinia
- Sass

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

再使用微信开发者工具导入 `dist/dev/mp-weixin` 或 `dist/build/mp-weixin`。

## 环境变量

- 默认不创建 `.env.local` 也能直接联调本地后端
- 需要通过 HTTPS 反代消除微信开发者工具里的 HTTP 警告时，再复制 `.env.example` 为 `.env.local`
- 当前可选变量：
  - `VITE_API_ORIGIN`

## 当前结构

```text
travel-miniapp/
├─ .env.example                     环境变量模板，定义小程序接口源站地址
├─ .env.local                       本机私有联调配置，由开发者自行创建，不提交 Git
├─ .gitignore                       忽略依赖、编译产物和本地环境文件
├─ eslint.config.mjs                ESLint Flat Config，约束 src 下 JS 代码规范
├─ package.json                     依赖与脚本入口，维护小程序编译与 lint 命令
├─ package-lock.json                npm 依赖锁文件，固定 Uni-app 相关依赖版本
├─ vite.config.js                   Uni-app 的 Vite 配置，挂载插件并配置 Sass 编译
└─ src/
   ├─ App.vue                       小程序根组件，承接全局生命周期与应用壳层
   ├─ main.js                       应用启动入口，注册 Pinia 和全局能力
   ├─ index.html                    H5 容器模板，主要服务 Uni-app 的 H5 构建场景
   ├─ manifest.json                 小程序平台配置，如 appid、图标和发行参数
   ├─ pages.json                    页面路由、导航栏和 tabBar 配置中心
   ├─ uni.scss                      全局 Sass 变量与主题样式入口
   ├─ api/                          按领域拆分的接口请求，如 auth、spot、order
   ├─ components/                   跨页面公共组件
   ├─ composables/                  可复用组合逻辑，如推荐流封装
   ├─ constants/
   │  └─ feature-entry-registry.js  功能入口注册表，统一维护“更多玩法”等入口元数据
   ├─ pages/                        页面业务域
   │  ├─ index/                     首页
   │  ├─ discover/                  发现页
   │  ├─ recommendation/            推荐页
   │  ├─ spot/                      景点
   │  ├─ guide/                     攻略
   │  ├─ order/                     订单
   │  ├─ mine/                      我的
   │  ├─ more/                      更多玩法
   │  ├─ random-pick/               随心一选
   │  ├─ budget-travel/             穷游玩法
   │  ├─ traveler-reviews/          游客口碑
   │  └─ trending-views/            近期热看
   ├─ services/                     聚合服务逻辑，面向玩法页拼装展示数据
   ├─ static/                       静态资源
   ├─ stores/
   │  └─ user.js                    用户全局状态，管理登录态与资料信息
   └─ utils/
      ├─ request.js                 通用请求封装，统一 baseURL、鉴权与错误处理
      ├─ spot-detail.js             景点详情跳转与参数拼装工具
      ├─ auth.js                    登录态与鉴权辅助工具
      ├─ location.js                定位与位置能力封装
      ├─ runtime-trace.js           运行时调试与问题定位辅助
      └─ util.js                    通用零散工具函数
```

页面模块内部按复杂度按需拆分：

```text
light-page/
└─ index.vue

feature-page/
├─ index.vue 或 list/detail.vue
├─ components/
├─ composables/                     可选
└─ services/                        可选
```

## 主要能力

- 微信登录、手机号绑定、资料与偏好管理
- 首页、发现页、推荐页、附近探索
- 景点列表、详情、搜索、地图导航、浏览足迹
- 攻略列表、详情、关联景点
- 收藏、评分评论、我的互动
- 订单创建、支付、取消、订单列表与详情
- 随心一选、穷游玩法、游客口碑、近期热看

## 开发约定

- 页面按业务域组织，不按展示位置组织
- 接口请求、聚合逻辑、页面渲染分层维护
- 新增注释统一使用中文，只在关键逻辑补充说明
- 景点详情跳转统一复用 `src/utils/spot-detail.js`

## 相关文档

- [仓库总览](../README.md)
- [开发期 HTTPS 与反代说明](../docs/dev-https-proxy.md)
- [需求文档](../docs/specs/travel-recommendation-system/requirements.md)

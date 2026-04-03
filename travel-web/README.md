# travel-web

WayTrip Web 用户端项目。

当前项目已经完成目录与页面架构重构，默认采用 `app / shared / modules` 三层结构继续演进，不再回退到旧的 `views / api / utils` 平铺写法。

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

默认开发地址：

```text
http://localhost:3001
```

生产构建：

```bash
npm run build
```

本地预览构建产物：

```bash
npm run preview
```

## 开发环境说明

Vite 开发服务器当前约定：

- Web 端端口：`3001`
- `/api` 代理到：`http://localhost:8080`
- `/uploads` 代理到：`http://localhost:8080`

对应配置见：

- [vite.config.js](/E:/Year-4/Grad-Project/WayTrip/travel-web/vite.config.js)

## 目录结构

当前源码目录：

```text
src/
├─ app/                              应用壳层
│  ├─ layouts/                       布局层
│  └─ router/                        路由层
├─ shared/                           共享层
│  ├─ api/                           公共请求能力
│  ├─ constants/                     共享常量
│  ├─ lib/                           通用工具
│  ├─ styles/                        全局样式
│  └─ ui/                            跨业务共享 UI
└─ modules/                          业务模块层
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
   └─ spot/
   ├─ traveler-reviews/
   └─ trending-views/
```

说明：

- `App.vue` 保持根壳组件职责
- `main.js` 负责应用初始化与插件接入
- 空骨架目录已经清理，不保留无实际内容的占位目录

## 分层职责

### `app`

只放应用壳层代码：

- 主站布局
- 认证布局
- 用户中心布局
- 路由定义
- 路由守卫

当前布局包括：

- `AppShellLayout`：主站浏览布局
- `AuthLayout`：登录注册布局
- `AccountLayout`：用户中心布局

### `shared`

只放真正跨业务复用的公共能力：

- `shared/api`：请求实例与拦截器
- `shared/constants`：应用名、路由常量、搜索热词等
- `shared/lib`：定位、足迹、冷启动引导等工具
- `shared/styles`：全局样式入口
- `shared/ui`：跨页面共享的探索类 UI 组件

如果一个文件有明显业务语义，就不应放进 `shared`。

### `modules`

所有业务页面、业务接口、模块私有组件和模块私有逻辑都放在这里，按业务域聚合。

当前 Web 端主要模块：

- `home`：首页
- `discover`：聚合探索页
- `search`：综合搜索页
- `spot`：景点列表与详情
- `guide`：攻略列表与详情
- `recommendation`：推荐结果页
- `nearby`：附近探索直达页
- `random-pick`：随心一选
- `budget-travel`：穷游玩法
- `traveler-reviews`：游客口碑
- `trending-views`：近期热看
- `more`：玩法入口聚合页
- `order`：订单相关页面
- `account`：个人中心域
- `auth`：登录注册域

## 当前页面架构

当前 Web 端页面组织遵循以下思路：

- 首页负责高频入口和首屏内容分发
- 发现页负责推荐、附近、景点、攻略的聚合探索
- 搜索页负责景点 + 攻略的综合检索
- 玩法型页面通过 `more` 与独立轻模块承接
- 用户中心统一收口到 `/account/*`
- 附近页保留为直达页，但主探索承接已并入发现页

当前账户中心路径包括：

```text
/account/profile
/account/activity
/account/settings
/account/orders
/account/orders/:id
/account/favorites
/account/reviews
```

## 模块组织约定

### 轻模块

简单模块保持轻量：

```text
module/
├─ api.js
└─ index.vue
```

### 多页面或复杂模块

复杂模块按需拆分：

```text
module/
├─ api.js
├─ components/
├─ composables/                      可选
├─ pages/
└─ store/                           可选
```

说明：

- 不为了“看起来规范”提前创建空目录
- 有真实职责再拆分，没有职责就保持轻量

## 命名规范

### 目录命名

- 统一小写
- 多单词目录使用 `kebab-case`

### 页面文件命名

- 轻模块页面使用 `index.vue`
- 多页面模块在 `pages/` 下使用语义文件名

例如：

- `modules/home/index.vue`
- `modules/spot/pages/list.vue`
- `modules/spot/pages/detail.vue`
- `modules/account/pages/profile.vue`

### 组件文件命名

- 统一使用 `PascalCase`
- 名称必须带明确业务语义

例如：

- `SpotCard.vue`
- `GuideCard.vue`
- `AccountPageHeader.vue`
- `ExploreKeywordGroup.vue`

### composable 命名

- 统一使用 `useXxx.js`

例如：

- `useRecommendationFeed.js`

## 开发约定

### 1. 新增代码先判断归属

新增文件前先判断它属于：

- 应用壳层
- 共享层
- 某个业务模块

不要把业务代码为了方便直接塞进 `shared`。

### 2. 不再新增旧结构目录

重构完成后，不再新增以下旧目录语义：

- `src/views`
- `src/api`
- `src/utils`
- `src/styles`
- `src/layout`
- `src/router`
- `src/stores`

### 3. 页面信息架构优先级高于局部堆功能

Web 端优先保证：

- 主导航清晰
- 页面职责不重叠
- 搜索、发现、首页的入口语言统一
- 用户中心路径与布局统一

不要为了补功能继续堆平行入口。

### 4. 注释要求

- 新增注释统一使用中文
- 只在关键逻辑、特殊分支、易误解位置补充简洁说明
- 注释重点解释“为什么这样做”
- 没有明确清理任务时，不随意删除原有注释

## 构建说明

当前构建已经做过基础分包优化：

- `element-plus`
- `@element-plus/icons-vue`
- `vue-router / pinia / vue`
- `axios`

仍可能看到 `element-plus` 大 chunk 警告，这是当前 UI 库体积带来的已知现象，不是本项目构建失败。

## 一句话原则

目录按业务聚合，公共能力集中管理，页面职责明确，空骨架不保留，Web 端体验优先保持首页、发现、搜索三条探索路径的一致性。

# travel-admin

WayTrip 管理端项目。

当前项目已经完成目录重构，采用轻量业务模块化结构。后续开发默认在这套结构上继续演进，不再回退到旧的全局平铺写法。

## 技术栈

- Vue 3
- Vite
- Element Plus
- Pinia
- Vue Router
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

## 目录结构

当前源码目录只保留三层：

```text
src/
├─ app/                              应用壳层
│  ├─ layout/                        布局
│  ├─ router/                        路由
│  └─ store/                         全局状态
├─ shared/                           共享层
│  ├─ api/                           公共请求能力
│  ├─ constants/                     全局常量
│  ├─ lib/                           通用工具
│  └─ styles/                        全局样式
└─ modules/                          业务模块层
   ├─ banner/                        轮播图管理
   ├─ category/                      分类管理
   ├─ overview/                      运营概览
   ├─ region/                        地区管理
   ├─ spot/                          景点管理
   ├─ guide/                         攻略管理
   ├─ order/                         订单中心
   ├─ recommendation/                推荐系统
   ├─ system/                        系统管理
   └─ user-ops/                      用户运营

模块内部按复杂度按需拆分：

light-module/
├─ api.js
└─ index.vue

feature-module/
├─ api.js 或 api/
├─ components/
├─ composables/                      可选
├─ pages/                            可选
└─ styles/                           可选
```

## 分层职责

### `app`

只放应用壳层代码：

- 入口文件
- 根组件
- 后台布局
- 路由
- 全局登录态

这里不放具体业务页面，也不放业务接口。

### `shared`

只放真正跨业务复用的公共能力：

- 请求实例
- 全局常量
- 通用工具
- 全局样式

如果一个文件带明显业务语义，就不要放在 `shared`。

### `modules`

按业务域组织代码。  
每个模块内部自己收口页面、接口、局部样式和页面逻辑，避免再次出现全局 `views / api / utils / styles` 平铺。

## 命名规范

### 目录命名

- 目录统一使用小写
- 多单词目录统一使用 `kebab-case`

示例：

- `user-ops`
- `view-source`

### 页面文件命名

- 轻模块：模块根目录 `index.vue + api.js`
- 进入 `pages/` 的列表页：统一使用 `xxx-list.vue`
- 配置页、总览页、登录页等非列表页：使用语义化页面名

示例：

- `modules/banner/index.vue`
- `modules/spot/pages/spot-list.vue`
- `modules/guide/pages/guide-list.vue`
- `modules/order/pages/order-center.vue`
- `modules/user-ops/pages/user-list.vue`
- `modules/system/pages/login.vue`
- `modules/recommendation/pages/config.vue`

### 组件文件命名

- 组件统一使用 `PascalCase`
- 组件名必须带业务语义

示例：

- `SpotFormDialog.vue`
- `RecommendationExecutionCard.vue`

### composable 文件命名

- 统一使用 `useXxx.js`
- 名称必须说明职责

示例：

- `useSpotOptions.js`
- `useRecommendationConfig.js`

### 接口文件命名

- 轻模块：模块根目录直接 `api.js`
- 多页面模块或接口较多的模块：使用 `api/` 目录拆分
- 如果模块已经引入 `pages/`，优先保持“页面与接口按模块聚合”，不要再回退到根目录平铺多个页面文件

示例：

- `modules/banner/api.js`
- `modules/system/api/auth.js`
- `modules/recommendation/api/recommendation.js`

## 开发约定

### 1. 新增代码先判断归属

新增文件前先判断它属于：

- 应用壳层
- 共享层
- 某个业务模块

不要为了图省事把业务代码放进 `shared`。

### 2. 轻模块保持轻量

轻模块保持：

```text
module/
  api.js
  index.vue
```

不要为了“看起来规范”提前拆很多空目录。

### 3. 中等及以上模块按需拆分

当页面明显膨胀，或者一个模块下已经出现多页面时，再往模块内拆：

- `pages/`
- `components/`
- `composables/`
- `api/`
- `styles/`

拆分的目标是降低维护成本，不是为了增加层级数量。

### 4. 不再回退到旧结构

重构完成后，不再新增以下旧目录语义：

- `src/views`
- `src/api`
- `src/utils`
- `src/styles`

也不再保留旧结构中转文件、兼容层、临时过渡写法。

### 5. 样式就近放置

- 全局样式放 `shared/styles`
- 模块私有样式放模块内部

如果样式只服务一个模块，就不要挂到全局目录。

### 6. 注释要求

- 新增注释统一使用中文
- 只在关键逻辑、特殊分支、易误解位置补充简洁说明
- 注释重点解释“为什么这样做”，不要翻译代码
- 非明确清理任务下，不要随意删除原有注释

## 开发建议

- 复杂模块在模块内按需拆分，简单模块保持轻量结构
- 多页面模块继续使用语义页面名和 `api/`
- 列表页统一使用 `xxx-list.vue`，避免进入 `pages/` 后继续保留无语义的 `index.vue`
- 不再引入旧的全局平铺目录，也不保留兼容层

## 修改前自检

1. 新文件是否放到了正确层级
2. 命名是否符合当前规则
3. 是否引入了不必要的兼容层或中转层
4. 新增关键逻辑是否补了中文注释
5. 构建是否通过

## 一句话原则

目录按业务聚合，公共能力集中管理，复杂模块适度拆分，简单模块保持克制。

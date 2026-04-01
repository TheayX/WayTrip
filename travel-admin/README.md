# travel-admin

## 目录结构

当前管理端采用轻量业务模块化结构，只保留三层：

```text
src/
  app/      应用壳层
  shared/   真正公共能力
  modules/  业务模块
```

### `src/app`

只放应用入口和壳层代码：

- `main.js`
- `App.vue`
- `layout/`
- `router/`
- `store/`

这里不放具体业务页面。

### `src/shared`

只放跨业务复用的公共能力：

- `api/`：请求实例
- `constants/`：全局常量
- `lib/`：通用工具
- `styles/`：全局样式

如果文件带明显业务语义，不要放到 `shared`。

### `src/modules`

按业务域组织代码。每个模块内部自收口，不再把页面、接口、样式分散到全局目录。

当前模块包括：

- `banner`
- `category`
- `dashboard`
- `guide`
- `order`
- `recommendation`
- `region`
- `spot`
- `system`
- `user-ops`

## 命名约定

### 目录命名

- 目录统一使用小写。
- 多单词目录使用 `kebab-case`，如 `user-ops`。

### 页面文件命名

- 单页面模块：使用 `index.vue`，如 `modules/spot/index.vue`
- 多页面模块：使用语义文件名，如 `login.vue`、`admin.vue`、`overview.vue`

不要在同一模块里混用无意义的页面名。

### 组件文件命名

- 组件统一使用 `PascalCase`
- 组件名要带业务语义，不要使用模糊命名

示例：

- `SpotFormDialog.vue`
- `RecommendationDebugCard.vue`

### 组合式函数命名

- 统一使用 `useXxx` 格式
- 文件名使用 `PascalCase` 语义前缀 + `use`

示例：

- `useSpotOptions.js`

### 接口文件命名

- 单页面简单模块：模块根下直接使用 `api.js`
- 多页面或接口较多的模块：使用 `api/` 目录拆分

示例：

- `modules/spot/api.js`
- `modules/system/api/auth.js`
- `modules/user-ops/api/view-log.js`

## 新增代码规则

### 1. 先判断归属，再新建文件

新增代码前先判断它属于：

- 应用壳层
- 公共层
- 某个业务模块

不要为了省事把业务代码塞进 `shared`。

### 2. 简单模块不硬拆

页面规模可控时，保持：

```text
module/
  index.vue
  api.js
```

不要为了“看起来规范”提前拆出一堆空目录。

### 3. 复杂模块再拆分

当页面明显变大时，再按需要拆：

- `components/`
- `composables/`
- `api/`

拆分目标是降低维护成本，不是为了追求层级数量。

### 4. 不保留旧结构兼容写法

重构后不再新增这类旧路径：

- `src/views`
- `src/api`
- `src/utils`

也不要为了兼容旧结构再做一层中转文件。

### 5. 样式就近放置

- 全局样式放 `shared/styles`
- 模块私有样式放模块内部

如果样式只服务某一个模块，就不要继续挂在全局目录。

## 当前项目的推荐做法

### 适合继续保持的写法

- `spot`、`recommendation` 这类复杂模块，继续在模块内拆组件
- `banner`、`category`、`region` 这类简单模块，继续保持轻量结构
- `user-ops`、`system` 这种多页面模块，继续用语义页面名和 `api/` 目录

### 不建议再做的事

- 不要重新引入全局 `views/api/utils` 平铺结构
- 不要为了统一而把所有简单模块强拆成很多层
- 不要保留“新旧结构并存”的过渡代码

## 一句话原则

目录按业务聚合，公共能力集中管理，复杂模块适度拆分，简单模块保持克制。

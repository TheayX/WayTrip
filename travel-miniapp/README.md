# travel-miniapp

## 目录结构

当前小程序按“页面业务域 + 公共能力”组织，适合当前项目体量，避免过深层级。

```text
travel-miniapp/
  src/
    api/
    components/
    constants/
    composables/
    pages/
    services/
    stores/
    utils/
```

## 分层说明

### `src/pages`

按页面业务域划分，是小程序的主要页面入口。

- `index/`
  首页与首页私有组件
- `spot/`
  景点相关页面
- `guide/`
  攻略相关页面
- `recommendation/`
  个性推荐页面
- `random-pick/`
  随心一选页面
- `budget-travel/`
  穷游玩法页面
- `traveler-reviews/`
  游客口碑页面
- `trending-views/`
  近期热看页面
- `more/`
  更多功能页
- `mine/`
  个人中心相关页面
- `order/`
  订单相关页面

说明：

1. 页面目录只使用业务语义，不使用“快捷导航”“feature”这类展示语义。
2. 页面私有组件可以继续放在各自页面目录下的 `components/` 中。

### `src/api`

只放接口请求方法，按后端领域划分。

例如：

- `api.js`
- `api.js`
- `api.js`
- `api.js`
- `api.js`

说明：

1. `api` 只负责请求，不承担页面聚合逻辑。
2. 当前项目不建议为了“更规范”再把 `api` 拆成更深目录。

### `src/services`

放跨接口的数据聚合逻辑，按业务模块划分。

当前已有：

- `random-pick.js`
- `budget-travel.js`
- `traveler-reviews.js`
- `trending-views.js`

说明：

1. 页面需要的聚合、筛选、整理逻辑优先放这里。
2. 后续补正式后端接口时，优先改 `services`，尽量不改页面结构。

### `src/constants`

放跨页面复用的常量与注册表。

当前已有：

- `feature-entry-registry.js`

说明：

1. 首页入口、更多页入口、预留入口统一由注册表管理。
2. “功能入口”是公共配置概念，不绑定首页展示位置。

### `src/components`

放跨页面复用的公共组件。

当前已有：

- `feature-entry/FeatureEntryGrid.vue`
- `PreferenceCategorySelector.vue`

说明：

1. 只有跨页面复用的组件才放这里。
2. 首页私有组件仍然留在 `pages/index/components/`，避免过早抽公共。

### `src/composables`

放可复用的组合式逻辑。

说明：

1. 适合封装页面复用状态和流程逻辑。
2. 例如首页推荐流这类逻辑，放在这里是合理的，不需要迁到别的目录。

### `src/stores`

放全局状态管理。

说明：

1. 用户状态、全局登录态等放这里。
2. `stores` 是正常顶层目录，不属于需要继续重构的对象。

### `src/utils`

放纯工具方法和通用辅助逻辑。

例如：

- 请求工具
- 图片地址处理
- 鉴权辅助
- 定位辅助

## 当前结构原则

1. 页面按业务域组织，不按展示位置组织。
2. 页面展示逻辑、接口请求、聚合逻辑分开。
3. 公共配置统一收口，避免首页和更多页各自维护一套。
4. 不为了“规范”引入过深目录层级。

## 景点详情来源参数

景点详情页统一通过 `src/utils/spot-detail.js` 构造跳转 URL，并传递 `source` 参数。

前端当前使用的来源值包括：

- `home`
- `list`
- `search`
- `nearby`
- `guide`
- `recommendation`
- `discover`
- `order`
- `similar`
- `random-pick`
- `budget-travel`
- `traveler-reviews`
- `trending-views`
- `footprint`
- `favorite`
- `review`
- `detail`

说明：

1. `home` 只表示首页来源，不作为全局默认值。
2. 景点详情页自身的兜底来源是 `detail`。
3. 前端保留页面级来源语义，方便后续做统计和排查。
4. 后端推荐权重会再做别名归类，不要求前端来源值和算法来源桶完全同名。

当前后端来源归类规则：

- 归到 `home`
  - `home`
- 归到 `search`
  - `search`
- 归到 `guide`
  - `guide`
- 归到 `recommendation`
  - `recommendation`
  - `discover`
  - `random-pick`
  - `budget-travel`
  - `traveler-reviews`
  - `trending-views`
- 归到 `detail`
  - `detail`
  - `list`
  - `nearby`
  - `similar`
  - `order`
  - `footprint`
  - `favorite`
  - `review`

维护约定：

1. 新增景点详情入口时，优先在 `src/utils/spot-detail.js` 增加来源常量。
2. 如果该来源需要影响推荐权重，再同步补后端别名归类。
3. 不要在页面里直接手写 `/pages/spot/detail?...&source=xxx`。

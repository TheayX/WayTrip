# travel-server

## 景点浏览来源归类

景点详情浏览上报接口：

- `POST /api/v1/spots/{spotId}/view`

前端会按页面入口传递 `source`，后端在推荐计算时再归类到有限的来源桶。  
归类逻辑当前实现在：

- `src/main/java/com/travel/service/impl/RecommendationServiceImpl.java`

### 前端可能上传的来源值

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

### 后端归类结果

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

### 维护约定

1. 新增景点详情入口时，先确定前端 `source` 命名是否有独立统计价值。
2. 如果只需要统计保留页面语义，前端可以新增 `source`，后端再决定归到哪个来源桶。
3. 如果新来源会影响推荐权重，必须同步更新 `RecommendationServiceImpl#normalizeViewSource`。
4. 不要把这类归类规则放到管理端页面，避免把开发约定和运营配置混在一起。

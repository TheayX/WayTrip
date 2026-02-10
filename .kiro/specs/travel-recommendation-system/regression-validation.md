# 关键业务流程回归验证（PR-5）

## 目标
验证软删除、发布状态与时间戳口径在核心链路中的一致性，确保用户端展示、管理端列表与统计结果一致。

## 验证范围
- 认证与用户资料：`/api/v1/auth/*`
- 景点/攻略/轮播图展示链路
- 收藏、评分、推荐、订单统计
- 分类筛选（扁平 + 树形）

## 检查项

### 1. 软删除过滤一致性
- 用户端列表与详情：仅返回 `is_deleted = 0`
- 仪表盘统计：排除 `is_deleted = 1`

### 2. 发布状态过滤一致性
- 用户端可见资源仅返回 `is_published = 1`
- 收藏/评分等用户动作禁止作用于未发布景点

### 3. 时间戳规则一致性
- 新增记录自动填充 `created_at`
- 存在 `updated_at` 的表在更新时自动维护

### 4. 分类接口兼容性
- `/api/v1/spots/filters` 返回：
  - `categories`（扁平）
  - `categoryTree`（树形）

## 本地静态回归命令

```bash
# 软删除与发布状态关键查询点扫描
rg -n "is_deleted|is_published|getPublished\(|getIsDeleted\(" travel-server/src/main/java/com/travel/service/impl

# 分类树字段与构建逻辑
rg -n "categoryTree|parentId|buildCategoryTree|getFilters" travel-server/src/main/java/com/travel

# 认证接口 phone 字段链路
rg -n "phone" travel-server/src/main/java/com/travel/dto/auth travel-server/src/main/java/com/travel/service/impl/AuthServiceImpl.java
```

## 构建验证

```bash
cd travel-server && mvn -q -DskipTests compile
```

> 若构建失败且错误为 Maven 中央仓库不可达（HTTP 403），判定为环境限制，不阻断口径文档验收。

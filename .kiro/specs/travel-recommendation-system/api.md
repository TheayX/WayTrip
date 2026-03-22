# API 接口文档

## 文档说明

- 对齐基线：当前控制器实现
- 更新时间：2026-03-22
- 说明：本版以 `travel-server/src/main/java/com/travel/controller` 为准，删除了与代码不一致的旧描述

## 基础约定

### 路由前缀

- 用户端：`/api/v1`
- 管理端：`/api/admin/v1`

### 认证方式

- 用户端：`Authorization: Bearer {token}`
- 管理端：`Authorization: Bearer {token}`

### 统一响应

```json
{
  "code": 0,
  "message": "success",
  "data": {},
  "timestamp": 1700000000000
}
```

### 分页响应

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "list": [],
    "total": 0,
    "page": 1,
    "pageSize": 10,
    "totalPages": 0
  },
  "timestamp": 1700000000000
}
```

## 用户端接口

### 1. 认证与资料

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/v1/auth/wx-login` | 微信登录 |
| POST | `/api/v1/auth/wx-bind-phone` | 小程序绑定手机号 |
| POST | `/api/v1/auth/web-register` | Web 注册 |
| POST | `/api/v1/auth/web-login` | Web 登录 |

### 2. 个人资料主路由

主入口统一为：

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/v1/user/info` | 获取用户信息 |
| PUT | `/api/v1/user/info` | 更新用户信息 |
| POST | `/api/v1/user/preferences` | 设置偏好标签 |
| PUT | `/api/v1/user/password` | 修改密码 |
| DELETE | `/api/v1/user/account` | 注销账户 |

### 3. 个人资料兼容路由

当前后端还保留了一组资料兼容接口：

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/v1/auth/user-info` | 获取用户信息 |
| PUT | `/api/v1/auth/user-info` | 更新用户信息 |
| POST | `/api/v1/auth/preferences` | 设置偏好标签 |
| PUT | `/api/v1/auth/password` | 修改密码 |

说明：

- `/api/v1/user/*` 是主入口。
- `/api/v1/auth/*` 资料接口仅作为兼容层保留。
- 新接入与现有前端统一使用 `/api/v1/user/*`，不要再混用。

### 4. 上传

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/v1/upload/avatar` | 上传头像 |

请求类型：

- `multipart/form-data`

### 5. 首页与推荐

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/v1/home/banners` | 获取首页轮播图 |
| GET | `/api/v1/home/hot` | 获取热门景点 |
| GET | `/api/v1/recommendations` | 获取个性化推荐 |
| POST | `/api/v1/recommendations/refresh` | 刷新推荐 |

### 6. 景点

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/v1/spots` | 景点列表 |
| GET | `/api/v1/spots/search` | 景点搜索 |
| GET | `/api/v1/spots/{spotId}` | 景点详情 |
| GET | `/api/v1/spots/filters` | 获取景点筛选项 |

### 7. 攻略

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/v1/guides` | 攻略列表 |
| GET | `/api/v1/guides/{guideId}` | 攻略详情 |
| GET | `/api/v1/guides/categories` | 攻略分类 |

### 8. 评分评论

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/v1/reviews` | 提交评分评论 |
| GET | `/api/v1/reviews/spot/{spotId}` | 获取当前用户对景点评分 |
| GET | `/api/v1/reviews/spot/{spotId}/comments` | 获取景点评论列表 |

### 9. 收藏

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/v1/favorites` | 添加收藏 |
| DELETE | `/api/v1/favorites/{spotId}` | 取消收藏 |
| GET | `/api/v1/favorites/check/{spotId}` | 检查收藏状态 |
| GET | `/api/v1/favorites` | 收藏列表 |

### 10. 订单

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/v1/orders` | 创建订单 |
| GET | `/api/v1/orders` | 用户订单列表 |
| GET | `/api/v1/orders/{id}` | 订单详情 |
| POST | `/api/v1/orders/{id}/pay` | 模拟支付 |
| POST | `/api/v1/orders/{id}/cancel` | 用户取消订单 |

## 管理端接口

### 1. 认证

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/admin/v1/auth/login` | 管理员登录 |
| GET | `/api/admin/v1/auth/info` | 获取管理员信息 |

### 2. 景点

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/admin/v1/spots` | 景点列表 |
| GET | `/api/admin/v1/spots/{spotId}` | 景点详情 |
| POST | `/api/admin/v1/spots` | 创建景点 |
| PUT | `/api/admin/v1/spots/{spotId}` | 更新景点 |
| PUT | `/api/admin/v1/spots/{spotId}/publish` | 更新发布状态 |
| DELETE | `/api/admin/v1/spots/{spotId}` | 删除景点 |
| GET | `/api/admin/v1/spots/filters` | 后台景点筛选项 |

### 3. 攻略

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/admin/v1/guides` | 攻略列表 |
| GET | `/api/admin/v1/guides/{guideId}` | 攻略详情 |
| GET | `/api/admin/v1/guides/categories` | 攻略分类 |
| POST | `/api/admin/v1/guides` | 创建攻略 |
| PUT | `/api/admin/v1/guides/{guideId}` | 更新攻略 |
| PUT | `/api/admin/v1/guides/{guideId}/publish` | 更新发布状态 |
| DELETE | `/api/admin/v1/guides/{guideId}` | 删除攻略 |

### 4. 订单

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/admin/v1/orders` | 订单列表 |
| GET | `/api/admin/v1/orders/{id}` | 订单详情 |
| POST | `/api/admin/v1/orders/{id}/complete` | 完成订单 |
| POST | `/api/admin/v1/orders/{id}/refund` | 退款订单 |
| POST | `/api/admin/v1/orders/{id}/cancel` | 取消待支付订单 |
| POST | `/api/admin/v1/orders/{id}/reopen` | 恢复已完成订单 |

### 5. 用户管理

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/admin/v1/users` | 用户列表 |
| GET | `/api/admin/v1/users/{id}` | 用户详情 |
| PUT | `/api/admin/v1/users/{id}/password` | 重置用户密码 |

### 6. 管理员管理

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/admin/v1/admins` | 管理员列表 |
| POST | `/api/admin/v1/admins` | 创建管理员 |
| PUT | `/api/admin/v1/admins/{id}` | 更新管理员 |
| PUT | `/api/admin/v1/admins/{id}/password` | 重置管理员密码 |
| DELETE | `/api/admin/v1/admins/{id}` | 删除管理员 |

### 7. 轮播图

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/admin/v1/banners` | 轮播图列表 |
| POST | `/api/admin/v1/banners` | 创建轮播图 |
| PUT | `/api/admin/v1/banners/{id}` | 更新轮播图 |
| DELETE | `/api/admin/v1/banners/{id}` | 删除轮播图 |
| POST | `/api/admin/v1/banners/{id}/toggle` | 切换启用状态 |

### 8. 仪表板

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/admin/v1/dashboard/overview` | 概览数据 |
| GET | `/api/admin/v1/dashboard/order-trend` | 订单趋势 |
| GET | `/api/admin/v1/dashboard/hot-spots` | 热门景点排行 |

### 9. 地区与分类

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/admin/v1/regions` | 地区列表 |
| POST | `/api/admin/v1/regions` | 创建地区 |
| PUT | `/api/admin/v1/regions/{id}` | 更新地区 |
| DELETE | `/api/admin/v1/regions/{id}` | 删除地区 |
| GET | `/api/admin/v1/categories` | 分类列表 |
| POST | `/api/admin/v1/categories` | 创建分类 |
| PUT | `/api/admin/v1/categories/{id}` | 更新分类 |
| DELETE | `/api/admin/v1/categories/{id}` | 删除分类 |

### 10. 上传

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/admin/v1/upload/image` | 上传图片 |
| POST | `/api/admin/v1/upload/icon` | 上传图标 |

## 关键约束

### 内容过滤

- 用户端默认只返回 `is_deleted=0`
- 用户端景点/攻略默认只返回已发布内容
- 首页轮播图默认只返回已启用内容

### 订单状态

| 值 | 含义 |
| --- | --- |
| 0 | 待支付 |
| 1 | 已支付 |
| 2 | 已取消 |
| 3 | 已退款 |
| 4 | 已完成 |

### 上传限制

- 头像：图片，2MB
- 管理端图片：图片，5MB
- 管理端图标：图片，2MB

## 当前文档化结论

- 资料接口现在已完成收口：`/api/v1/user/*` 为主入口，`/api/v1/auth/*` 为兼容层。
- 管理端图片上传接口和图标上传接口已实际存在，不应再标记为待实现。
- 如果后续继续演进接口，建议直接以 Knife4j 导出结果为准同步更新该文档。

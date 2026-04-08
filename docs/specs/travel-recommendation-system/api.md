# API 文档

## 文档说明

- 对齐基线：当前控制器实现
- 更新时间：2026-04-08
- 说明：本版同步四端重构后的接口现状，补齐附近景点、相似景点、浏览记录、用户洞察与管理端最新路径

## 用户端接口

### 1. 认证与资料

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/v1/auth/wx-login` | 微信登录 |
| POST | `/api/v1/auth/wx-bind-phone` | 微信绑定手机号 |
| POST | `/api/v1/auth/wx-prepare-bind-phone` | 微信绑定前校验 |
| POST | `/api/v1/auth/web-register` | Web 注册 |
| POST | `/api/v1/auth/web-prepare-register` | Web 注册前校验 |
| POST | `/api/v1/auth/web-login` | Web 登录 |
| GET | `/api/v1/user/info` | 获取用户信息 |
| PUT | `/api/v1/user/info` | 更新用户信息 |
| POST | `/api/v1/user/preferences` | 保存偏好标签 |
| PUT | `/api/v1/user/password` | 修改密码 |
| DELETE | `/api/v1/user/account` | 注销账户 |

说明：

- 当前资料接口主入口是 `/api/v1/user/*`
- `/api/v1/auth/*` 只承载认证与兼容能力

### 2. 文件上传

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/v1/upload/avatar` | 上传头像 |

请求类型：

- `multipart/form-data`

### 3. 首页与推荐

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/v1/home/banners` | 获取首页轮播图 |
| GET | `/api/v1/home/hot` | 获取热门景点 |
| GET | `/api/v1/home/nearby` | 获取附近景点 |
| GET | `/api/v1/recommendations` | 获取个性化推荐 |
| POST | `/api/v1/recommendations/refresh` | 刷新推荐 |
| GET | `/api/v1/recommendations/similar` | 获取相似景点 |

### 4. 景点

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/v1/spots` | 景点列表 |
| GET | `/api/v1/spots/search` | 景点搜索 |
| GET | `/api/v1/spots/views` | 获取浏览记录列表 |
| GET | `/api/v1/spots/{spotId}` | 景点详情 |
| POST | `/api/v1/spots/{spotId}/view` | 记录景点浏览行为 |
| GET | `/api/v1/spots/filters` | 获取景点筛选项 |

### 5. 攻略

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/v1/guides` | 攻略列表 |
| GET | `/api/v1/guides/{guideId}` | 攻略详情 |
| GET | `/api/v1/guides/categories` | 攻略分类 |

### 6. 评分评论

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/v1/reviews` | 提交评分评论 |
| GET | `/api/v1/reviews/spot/{spotId}` | 获取当前用户对景点评分 |
| GET | `/api/v1/reviews/spot/{spotId}/comments` | 获取景点评论列表 |
| GET | `/api/v1/reviews/mine` | 获取我的评价列表 |
| DELETE | `/api/v1/reviews/{reviewId}` | 删除我的评价 |

### 7. 收藏

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/v1/favorites` | 添加收藏 |
| DELETE | `/api/v1/favorites/{spotId}` | 取消收藏 |
| GET | `/api/v1/favorites/check/{spotId}` | 检查收藏状态 |
| GET | `/api/v1/favorites` | 收藏列表 |

### 8. 订单

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
| POST | `/api/admin/v1/spots/{spotId}/rating/refresh` | 刷新单个景点评分聚合 |
| POST | `/api/admin/v1/spots/rating/refresh` | 刷新全部景点评分聚合 |
| POST | `/api/admin/v1/spots/{spotId}/heat/refresh` | 刷新单个景点热度 |
| POST | `/api/admin/v1/spots/heat/refresh` | 刷新全部景点热度 |
| GET | `/api/admin/v1/spots/filters` | 获取后台景点筛选项 |

### 3. 攻略

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/admin/v1/guides` | 攻略列表 |
| GET | `/api/admin/v1/guides/{guideId}` | 攻略详情 |
| GET | `/api/admin/v1/guides/categories` | 攻略分类选项 |
| POST | `/api/admin/v1/guides` | 创建攻略 |
| PUT | `/api/admin/v1/guides/{guideId}` | 更新攻略 |
| PUT | `/api/admin/v1/guides/{guideId}/publish` | 更新发布状态 |
| DELETE | `/api/admin/v1/guides/{guideId}` | 删除攻略 |

### 4. 订单

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/admin/v1/orders` | 后台订单列表 |
| GET | `/api/admin/v1/orders/{id}` | 后台订单详情 |
| POST | `/api/admin/v1/orders/{id}/complete` | 标记完成 |
| POST | `/api/admin/v1/orders/{id}/refund` | 执行退款 |
| POST | `/api/admin/v1/orders/{id}/cancel` | 后台取消 |
| POST | `/api/admin/v1/orders/{id}/reopen` | 恢复为已支付 |

### 5. 用户、管理员与用户洞察

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/admin/v1/users` | 用户列表 |
| GET | `/api/admin/v1/users/{id}` | 用户详情 |
| PUT | `/api/admin/v1/users/{id}/password` | 重置用户密码 |
| GET | `/api/admin/v1/admins` | 管理员列表 |
| POST | `/api/admin/v1/admins` | 创建管理员 |
| PUT | `/api/admin/v1/admins/{id}` | 更新管理员 |
| PUT | `/api/admin/v1/admins/{id}/password` | 重置管理员密码 |
| DELETE | `/api/admin/v1/admins/{id}` | 删除管理员 |
| GET | `/api/admin/v1/user-insights/preferences` | 用户偏好洞察 |
| GET | `/api/admin/v1/user-insights/favorites` | 用户收藏洞察 |
| DELETE | `/api/admin/v1/user-insights/favorites/{id}` | 删除异常收藏记录 |
| GET | `/api/admin/v1/user-insights/views` | 用户浏览洞察 |
| DELETE | `/api/admin/v1/user-insights/views/{id}` | 删除异常浏览记录 |

### 6. 地区、分类、轮播图、上传

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
| GET | `/api/admin/v1/banners` | 轮播图列表 |
| POST | `/api/admin/v1/banners` | 创建轮播图 |
| PUT | `/api/admin/v1/banners/{id}` | 更新轮播图 |
| DELETE | `/api/admin/v1/banners/{id}` | 删除轮播图 |
| POST | `/api/admin/v1/banners/{id}/toggle` | 切换轮播图启用状态 |
| POST | `/api/admin/v1/upload/image` | 上传图片 |
| POST | `/api/admin/v1/upload/icon` | 上传图标 |

### 7. 仪表板与推荐系统

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/admin/v1/dashboard/overview` | 获取概览卡片数据 |
| GET | `/api/admin/v1/dashboard/order-trend` | 获取订单趋势 |
| GET | `/api/admin/v1/dashboard/hot-spots` | 获取热门景点排行 |
| POST | `/api/admin/v1/recommendation/update-matrix` | 手动重建相似度矩阵 |
| GET | `/api/admin/v1/recommendation/config` | 获取推荐配置 |
| PUT | `/api/admin/v1/recommendation/config` | 更新推荐配置 |
| GET | `/api/admin/v1/recommendation/status` | 获取推荐引擎运行状态 |
| GET | `/api/admin/v1/recommendation/preview` | 调试预览指定用户推荐结果 |
| GET | `/api/admin/v1/recommendation/similarity-preview` | 预览指定景点的相似邻居 |

## 当前文档化结论

- 当前 API 已覆盖四端主链路，不再只是“基础 CRUD + 两个推荐接口”的早期状态。
- 用户端与管理端的推荐能力都已成为正式接口能力的一部分。
- 若后续继续调整接口，应优先以控制器实现与 SpringDoc / Swagger UI 为准同步回填。

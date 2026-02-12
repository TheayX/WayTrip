# API 接口设计文档

## 概述

本文档描述旅游推荐系统的 RESTful API 接口设计，包括用户端（Uni-app 小程序）和管理端（Web 后台）的所有接口。

## 基础信息

### 基础 URL

```
用户端 API: /api/v1
管理端 API: /api/admin/v1
```

### 请求格式

- Content-Type: `application/json`
- 字符编码: `UTF-8`

### 认证方式

- 用户端: JWT Token，通过 Header `Authorization: Bearer {token}` 传递
- 管理端: JWT Token，通过 Header `Authorization: Bearer {token}` 传递

### 统一响应格式

```json
{
  "code": 0,
  "message": "success",
  "data": {},
  "timestamp": 1704067200000
}
```

| 字段      | 类型   | 说明                               |
| --------- | ------ | ---------------------------------- |
| code      | int    | 业务状态码，0表示成功，非0表示失败 |
| message   | string | 提示信息                           |
| data      | object | 响应数据                           |
| timestamp | long   | 服务器时间戳                       |

### 分页响应格式

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "list": [],
    "total": 100,
    "page": 1,
    "pageSize": 10,
    "totalPages": 10
  },
  "timestamp": 1704067200000
}
```

### 错误码定义

| 错误码 | HTTP状态码 | 说明                 |
| ------ | ---------- | -------------------- |
| 0      | 200        | 成功                 |
| 10001  | 401        | 微信登录失败         |
| 10002  | 401        | Token无效或过期      |
| 10003  | 403        | 无权限访问           |
| 20001  | 404        | 景点不存在           |
| 20002  | 400        | 景点已下架           |
| 30001  | 404        | 攻略不存在           |
| 30002  | 400        | 攻略已下架           |
| 40001  | 404        | 订单不存在           |
| 40002  | 400        | 订单状态不允许此操作 |
| 40003  | 400        | 订单已支付           |
| 40004  | 400        | 订单已取消           |
| 50001  | 400        | 评分值无效           |
| 60001  | 400        | 参数校验失败         |
| 60002  | 500        | 服务器内部错误       |

### 数据口径与过滤规则（PR-5）

为保持用户端展示、管理端列表与统计数据一致，接口默认遵循以下规则：

1. 软删除过滤：业务读取默认 `is_deleted = 0`。
2. 发布过滤：用户端可见内容默认 `is_published = 1`。
3. 时间戳语义：
   - `created_at`：创建时间；
   - `updated_at`：更新时间（仅在存在该字段的表维护）；
   - 无 `updated_at` 表仅依赖 `created_at`。
4. 兼容策略：新增字段采用向后兼容方式，不移除旧字段。

---

## 用户端 API

### 1. 认证模块

#### 1.1 微信登录

用户通过微信小程序登录。

**请求**

```
POST /api/v1/auth/wx-login
```

**请求体**

```json
{
  "code": "wx_login_code_xxx"
}
```

| 参数 | 类型   | 必填 | 说明                         |
| ---- | ------ | ---- | ---------------------------- |
| code | string | 是   | 微信登录凭证（wx.login获取） |

**响应**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "expiresIn": 604800,
    "user": {
      "id": 1,
      "nickname": "用户昵称",
      "avatar": "https://example.com/avatar.jpg",
      "phone": "13800000000",
      "isNewUser": false
    }
  }
}
```

#### 1.2 获取用户信息

获取当前登录用户信息。

**请求**

```
GET /api/v1/auth/user-info
```

**响应**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": 1,
    "nickname": "用户昵称",
    "avatar": "https://example.com/avatar.jpg",
    "phone": "13800000000",
    "preferences": ["自然风光", "历史文化"]
  }
}
```

#### 1.3 更新用户信息

更新用户昵称、头像和手机号。

**请求**

```
PUT /api/v1/auth/user-info
```

**请求体**

```json
{
  "nickname": "新昵称",
  "avatar": "https://example.com/new-avatar.jpg",
  "phone": "13800000000"
}
```

| 参数     | 类型   | 必填 | 说明                       |
| -------- | ------ | ---- | -------------------------- |
| nickname | string | 否   | 用户昵称                   |
| avatar   | string | 否   | 头像URL                    |
| phone    | string | 否   | 手机号（可传空字符串清空） |

**响应**

```json
{
  "code": 0,
  "message": "success",
  "data": null
}
```

#### 1.4 设置用户偏好标签

设置用户偏好标签（用于冷启动推荐）。

**请求**

```
POST /api/v1/auth/preferences
```

**请求体**

```json
{
  "tags": ["自然风光", "历史文化", "主题乐园"]
}
```

**响应**

```json
{
  "code": 0,
  "message": "success",
  "data": null
}
```

---

### 2. 首页模块

#### 2.1 获取轮播图列表

获取首页轮播图。

**请求**

```
GET /api/v1/home/banners
```

**响应**

```json
{
  "code": 0,
  "message": "success",
  "data": [
    {
      "id": 1,
      "imageUrl": "https://example.com/banner1.jpg",
      "spotId": 101,
      "spotName": "故宫博物院"
    }
  ]
}
```

#### 2.2 获取热门推荐

获取基于全局热度的热门景点。

**请求**

```
GET /api/v1/home/hot?limit=10
```

| 参数  | 类型 | 必填 | 说明             |
| ----- | ---- | ---- | ---------------- |
| limit | int  | 否   | 返回数量，默认10 |

**响应**

```json
{
  "code": 0,
  "message": "success",
  "data": [
    {
      "id": 101,
      "name": "故宫博物院",
      "coverImage": "https://example.com/spot1.jpg",
      "price": 60.0,
      "avgRating": 4.8,
      "heatScore": 9999
    }
  ]
}
```

---

### 3. 推荐模块

#### 3.1 获取个性化推荐

获取基于 ItemCF 的个性化推荐。

**请求**

```
GET /api/v1/recommendations?limit=10
```

| 参数  | 类型 | 必填 | 说明             |
| ----- | ---- | ---- | ---------------- |
| limit | int  | 否   | 返回数量，默认10 |

**响应**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "type": "personalized",
    "hasEnoughInteractions": true,
    "spots": [
      {
        "id": 102,
        "name": "颐和园",
        "coverImage": "https://example.com/spot2.jpg",
        "price": 30.0,
        "avgRating": 4.7,
        "recommendScore": 0.85
      }
    ],
    "showPreferenceGuide": false
  }
}
```

| 字段                  | 说明                                                                |
| --------------------- | ------------------------------------------------------------------- |
| type                  | 推荐类型：personalized（个性化）/ hot（热门）/ cold_start（冷启动） |
| hasEnoughInteractions | 是否有足够交互记录                                                  |
| showPreferenceGuide   | 是否显示偏好标签引导                                                |

#### 3.2 刷新推荐

刷新获取新一批推荐结果。

**请求**

```
POST /api/v1/recommendations/refresh?limit=10
```

**响应**

同 3.1

---

### 4. 景点模块

#### 4.1 获取景点列表

分页获取景点列表，支持筛选和排序。

**请求**

```
GET /api/v1/spots?page=1&pageSize=10&regionId=1&categoryId=2&sortBy=heat
```

| 参数       | 类型   | 必填 | 说明                                                                               |
| ---------- | ------ | ---- | ---------------------------------------------------------------------------------- |
| page       | int    | 否   | 页码，默认1                                                                        |
| pageSize   | int    | 否   | 每页数量，默认10                                                                   |
| regionId   | long   | 否   | 地区ID筛选                                                                         |
| categoryId | long   | 否   | 分类ID筛选                                                                         |
| sortBy     | string | 否   | 排序方式：heat（热度）/rating（评分）/price_asc（价格升序）/price_desc（价格降序） |

**响应**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "list": [
      {
        "id": 101,
        "name": "故宫博物院",
        "coverImage": "https://example.com/spot1.jpg",
        "price": 60.0,
        "avgRating": 4.8,
        "ratingCount": 1234,
        "regionName": "北京",
        "categoryName": "历史文化"
      }
    ],
    "total": 100,
    "page": 1,
    "pageSize": 10,
    "totalPages": 10
  }
}
```

#### 4.2 搜索景点

根据关键词搜索景点。

**请求**

```
GET /api/v1/spots/search?keyword=故宫&page=1&pageSize=10
```

| 参数     | 类型   | 必填 | 说明             |
| -------- | ------ | ---- | ---------------- |
| keyword  | string | 是   | 搜索关键词       |
| page     | int    | 否   | 页码，默认1      |
| pageSize | int    | 否   | 每页数量，默认10 |

**响应**

同 4.1，当无结果时 list 为空数组

#### 4.3 获取景点详情

获取景点详细信息。

**请求**

```
GET /api/v1/spots/{spotId}
```

**响应**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": 101,
    "name": "故宫博物院",
    "description": "故宫博物院是中国最大的古代文化艺术博物馆...",
    "price": 60.0,
    "openTime": "08:30-17:00（16:00停止入场）",
    "address": "北京市东城区景山前街4号",
    "latitude": 39.916345,
    "longitude": 116.397155,
    "images": [
      "https://example.com/spot1-1.jpg",
      "https://example.com/spot1-2.jpg"
    ],
    "avgRating": 4.8,
    "ratingCount": 1234,
    "regionName": "北京",
    "categoryName": "历史文化",
    "isFavorite": false,
    "userRating": null,
    "latestComments": [
      {
        "id": 1,
        "nickname": "游客A",
        "avatar": "https://example.com/avatar1.jpg",
        "score": 5,
        "comment": "非常值得一去！",
        "createdAt": "2025-01-01 10:00:00"
      }
    ]
  }
}
```

#### 4.4 获取筛选选项

获取地区、扁平分类列表和分类树（parent_id 层级结构）。

**请求**

```
GET /api/v1/spots/filters
```

**响应**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "regions": [
      { "id": 1, "name": "北京" },
      { "id": 2, "name": "上海" }
    ],
    "categories": [
      {
        "id": 1,
        "name": "自然风光",
        "parentId": 0,
        "iconUrl": "https://example.com/icon-nature.png",
        "children": []
      },
      {
        "id": 2,
        "name": "山岳",
        "parentId": 1,
        "iconUrl": "https://example.com/icon-mountain.png",
        "children": []
      }
    ],
    "categoryTree": [
      {
        "id": 1,
        "name": "自然风光",
        "parentId": 0,
        "iconUrl": "https://example.com/icon-nature.png",
        "children": [
          {
            "id": 2,
            "name": "山岳",
            "parentId": 1,
            "iconUrl": "https://example.com/icon-mountain.png",
            "children": []
          }
        ]
      }
    ]
  }
}
```

---

### 5. 攻略模块

#### 5.1 获取攻略列表

分页获取攻略列表。

**请求**

```
GET /api/v1/guides?page=1&pageSize=10&category=美食&sortBy=time
```

| 参数     | 类型   | 必填 | 说明                                    |
| -------- | ------ | ---- | --------------------------------------- |
| page     | int    | 否   | 页码，默认1                             |
| pageSize | int    | 否   | 每页数量，默认10                        |
| category | string | 否   | 分类筛选                                |
| sortBy   | string | 否   | 排序方式：time（时间）/category（分类） |

**响应**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "list": [
      {
        "id": 1,
        "title": "北京三日游攻略",
        "coverImage": "https://example.com/guide1.jpg",
        "category": "行程规划",
        "viewCount": 1234,
        "createdAt": "2025-01-01 10:00:00"
      }
    ],
    "total": 50,
    "page": 1,
    "pageSize": 10,
    "totalPages": 5
  }
}
```

#### 5.2 获取攻略详情

获取攻略详细内容。

**请求**

```
GET /api/v1/guides/{guideId}
```

**响应**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": 1,
    "title": "北京三日游攻略",
    "content": "<p>北京是中国的首都...</p>",
    "coverImage": "https://example.com/guide1.jpg",
    "category": "行程规划",
    "viewCount": 1235,
    "createdAt": "2025-01-01 10:00:00",
    "relatedSpots": [
      {
        "id": 101,
        "name": "故宫博物院",
        "coverImage": "https://example.com/spot1.jpg",
        "price": 60.0
      }
    ]
  }
}
```

#### 5.3 获取攻略分类列表

获取所有攻略分类。

**请求**

```
GET /api/v1/guides/categories
```

**响应**

```json
{
  "code": 0,
  "message": "success",
  "data": ["行程规划", "美食推荐", "住宿攻略", "交通指南"]
}
```

---

### 6. 评分模块

#### 6.1 提交评分

为景点提交评分和评论。

**请求**

```
POST /api/v1/ratings
```

**请求体**

```json
{
  "spotId": 101,
  "score": 5,
  "comment": "非常值得一去！"
}
```

| 参数    | 类型   | 必填 | 说明        |
| ------- | ------ | ---- | ----------- |
| spotId  | long   | 是   | 景点ID      |
| score   | int    | 是   | 评分（1-5） |
| comment | string | 否   | 评论内容    |

**响应**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": 1,
    "spotId": 101,
    "score": 5,
    "comment": "非常值得一去！",
    "createdAt": "2025-01-01 10:00:00"
  }
}
```

#### 6.2 获取用户对景点的评分

获取当前用户对指定景点的评分。

**请求**

```
GET /api/v1/ratings/spot/{spotId}
```

**响应**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": 1,
    "score": 5,
    "comment": "非常值得一去！",
    "createdAt": "2025-01-01 10:00:00"
  }
}
```

如果用户未评分，data 为 null

#### 6.3 获取景点评论列表

获取景点的评论列表。

**请求**

```
GET /api/v1/ratings/spot/{spotId}/comments?page=1&pageSize=10
```

**响应**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "list": [
      {
        "id": 1,
        "userId": 1,
        "nickname": "游客A",
        "avatar": "https://example.com/avatar1.jpg",
        "score": 5,
        "comment": "非常值得一去！",
        "createdAt": "2025-01-01 10:00:00"
      }
    ],
    "total": 100,
    "page": 1,
    "pageSize": 10,
    "totalPages": 10
  }
}
```

---

### 7. 收藏模块

#### 7.1 添加收藏

将景点添加到收藏。

**请求**

```
POST /api/v1/favorites
```

**请求体**

```json
{
  "spotId": 101
}
```

**响应**

```json
{
  "code": 0,
  "message": "success",
  "data": null
}
```

#### 7.2 取消收藏

取消收藏景点。

**请求**

```
DELETE /api/v1/favorites/{spotId}
```

**响应**

```json
{
  "code": 0,
  "message": "success",
  "data": null
}
```

#### 7.3 获取收藏列表

获取用户的收藏列表。

**请求**

```
GET /api/v1/favorites?page=1&pageSize=10
```

**响应**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "list": [
      {
        "id": 101,
        "name": "故宫博物院",
        "coverImage": "https://example.com/spot1.jpg",
        "price": 60.0,
        "avgRating": 4.8,
        "favoritedAt": "2025-01-01 10:00:00"
      }
    ],
    "total": 20,
    "page": 1,
    "pageSize": 10,
    "totalPages": 2
  }
}
```

#### 7.4 检查收藏状态

检查景点是否已收藏。

**请求**

```
GET /api/v1/favorites/check/{spotId}
```

**响应**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "isFavorite": true
  }
}
```

---

### 8. 订单模块

#### 8.1 创建订单

创建门票订单。

**请求**

```
POST /api/v1/orders
```

**请求体**

```json
{
  "spotId": 101,
  "visitDate": "2025-02-01",
  "quantity": 2,
  "contactName": "张三",
  "contactPhone": "13800138000",
  "idempotentKey": "uuid-xxx-xxx"
}
```

| 参数          | 类型   | 必填 | 说明                   |
| ------------- | ------ | ---- | ---------------------- |
| spotId        | long   | 是   | 景点ID                 |
| visitDate     | string | 是   | 游玩日期（YYYY-MM-DD） |
| quantity      | int    | 是   | 数量                   |
| contactName   | string | 是   | 联系人姓名             |
| contactPhone  | string | 是   | 联系人电话             |
| idempotentKey | string | 否   | 幂等键（防重复提交）   |

**响应**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": 1,
    "orderNo": "ORD20250101100000001",
    "spotId": 101,
    "spotName": "故宫博物院",
    "spotImage": "https://example.com/spot1.jpg",
    "unitPrice": 60.0,
    "quantity": 2,
    "totalPrice": 120.0,
    "visitDate": "2025-02-01",
    "contactName": "张三",
    "contactPhone": "13800138000",
    "status": "PENDING_PAYMENT",
    "statusText": "待支付",
    "createdAt": "2025-01-01 10:00:00"
  }
}
```

#### 8.2 获取订单列表

获取用户订单列表。

**请求**

```
GET /api/v1/orders?page=1&pageSize=10&status=0
```

| 参数     | 类型 | 必填 | 说明                                                   |
| -------- | ---- | ---- | ------------------------------------------------------ |
| page     | int  | 否   | 页码，默认1                                            |
| pageSize | int  | 否   | 每页数量，默认10                                       |
| status   | int  | 否   | 状态筛选：0-待支付/1-已支付/2-已取消/3-已退款/4-已完成 |

**响应**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "list": [
      {
        "id": 1,
        "orderNo": "ORD20250101100000001",
        "spotName": "故宫博物院",
        "spotImage": "https://example.com/spot1.jpg",
        "totalPrice": 120.0,
        "quantity": 2,
        "visitDate": "2025-02-01",
        "status": "PENDING_PAYMENT",
        "statusText": "待支付",
        "createdAt": "2025-01-01 10:00:00"
      }
    ],
    "total": 10,
    "page": 1,
    "pageSize": 10,
    "totalPages": 1
  }
}
```

#### 8.3 获取订单详情

获取订单详细信息。

**请求**

```
GET /api/v1/orders/{orderId}
```

**响应**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": 1,
    "orderNo": "ORD20250101100000001",
    "spotId": 101,
    "spotName": "故宫博物院",
    "spotImage": "https://example.com/spot1.jpg",
    "unitPrice": 60.0,
    "quantity": 2,
    "totalPrice": 120.0,
    "visitDate": "2025-02-01",
    "contactName": "张三",
    "contactPhone": "13800138000",
    "status": "PENDING_PAYMENT",
    "statusText": "待支付",
    "createdAt": "2025-01-01 10:00:00",
    "paidAt": null,
    "cancelledAt": null,
    "refundedAt": null,
    "completedAt": null
  }
}
```

#### 8.4 模拟支付

模拟支付订单。

**请求**

```
POST /api/v1/orders/{orderId}/pay
```

**响应**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "orderId": 1,
    "orderNo": "ORD20250101100000001",
    "status": "PAID",
    "statusText": "已支付",
    "paidAt": "2025-01-01 10:05:00"
  }
}
```

#### 8.5 取消订单

取消订单。

**请求**

```
POST /api/v1/orders/{orderId}/cancel
```

**响应**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "orderId": 1,
    "orderNo": "ORD20250101100000001",
    "status": "CANCELLED",
    "statusText": "已取消",
    "cancelledAt": "2025-01-01 10:10:00"
  }
}
```

---

## 管理端 API

### 1. 管理员认证

#### 1.1 管理员登录

**请求**

```
POST /api/admin/v1/auth/login
```

**请求体**

```json
{
  "username": "admin",
  "password": "admin123"
}
```

**响应**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "expiresIn": 86400,
    "admin": {
      "id": 1,
      "username": "admin",
      "realName": "系统管理员"
    }
  }
}
```

#### 1.2 获取管理员信息

**请求**

```
GET /api/admin/v1/auth/info
```

**响应**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": 1,
    "username": "admin",
    "realName": "系统管理员",
    "lastLoginAt": "2025-01-01 09:00:00"
  }
}
```

---

### 2. 景点管理

#### 2.1 获取景点列表

**请求**

```
GET /api/admin/v1/spots?page=1&pageSize=10&keyword=故宫&published=1
```

| 参数       | 类型   | 必填 | 说明                         |
| ---------- | ------ | ---- | ---------------------------- |
| page       | int    | 否   | 页码，默认1                  |
| pageSize   | int    | 否   | 每页数量，默认10             |
| keyword    | string | 否   | 关键词搜索                   |
| regionId   | long   | 否   | 地区筛选                     |
| categoryId | long   | 否   | 分类筛选                     |
| published  | int    | 否   | 发布状态：0-未发布，1-已发布 |

**响应**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "list": [
      {
        "id": 101,
        "name": "故宫博物院",
        "coverImage": "https://example.com/spot1.jpg",
        "price": 60.0,
        "regionName": "北京",
        "categoryName": "历史文化",
        "avgRating": 4.8,
        "heatScore": 9999,
        "published": true,
        "createdAt": "2025-01-01 10:00:00"
      }
    ],
    "total": 100,
    "page": 1,
    "pageSize": 10,
    "totalPages": 10
  }
}
```

#### 2.2 获取景点详情

**请求**

```
GET /api/admin/v1/spots/{spotId}
```

**响应**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": 101,
    "name": "故宫博物院",
    "description": "故宫博物院是中国最大的古代文化艺术博物馆...",
    "price": 60.0,
    "openTime": "08:30-17:00（16:00停止入场）",
    "address": "北京市东城区景山前街4号",
    "latitude": 39.916345,
    "longitude": 116.397155,
    "coverImage": "https://example.com/spot1.jpg",
    "images": [
      "https://example.com/spot1-1.jpg",
      "https://example.com/spot1-2.jpg"
    ],
    "regionId": 1,
    "categoryId": 2,
    "published": true,
    "createdAt": "2025-01-01 10:00:00",
    "updatedAt": "2025-01-01 10:00:00"
  }
}
```

#### 2.3 创建景点

**请求**

```
POST /api/admin/v1/spots
```

**请求体**

```json
{
  "name": "故宫博物院",
  "description": "故宫博物院是中国最大的古代文化艺术博物馆...",
  "price": 60.0,
  "openTime": "08:30-17:00（16:00停止入场）",
  "address": "北京市东城区景山前街4号",
  "latitude": 39.916345,
  "longitude": 116.397155,
  "coverImage": "https://example.com/spot1.jpg",
  "images": ["https://example.com/spot1-1.jpg"],
  "regionId": 1,
  "categoryId": 2,
  "published": false
}
```

| 参数        | 类型    | 必填 | 说明                |
| ----------- | ------- | ---- | ------------------- |
| name        | string  | 是   | 景点名称            |
| description | string  | 否   | 景点简介            |
| price       | decimal | 是   | 门票价格            |
| openTime    | string  | 否   | 开放时间            |
| address     | string  | 是   | 详细地址            |
| latitude    | decimal | 是   | 纬度                |
| longitude   | decimal | 是   | 经度                |
| coverImage  | string  | 否   | 封面图URL           |
| images      | array   | 否   | 图片URL列表         |
| regionId    | long    | 否   | 地区ID              |
| categoryId  | long    | 否   | 分类ID              |
| published   | boolean | 否   | 是否发布，默认false |

**响应**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": 101
  }
}
```

#### 2.4 更新景点

**请求**

```
PUT /api/admin/v1/spots/{spotId}
```

**请求体**

同创建景点，所有字段可选

**响应**

```json
{
  "code": 0,
  "message": "success",
  "data": null
}
```

#### 2.5 更新发布状态

**请求**

```
PUT /api/admin/v1/spots/{spotId}/publish
```

**请求体**

```json
{
  "published": true
}
```

**响应**

```json
{
  "code": 0,
  "message": "success",
  "data": null
}
```

#### 2.6 删除景点

**请求**

```
DELETE /api/admin/v1/spots/{spotId}
```

**响应**

```json
{
  "code": 0,
  "message": "success",
  "data": null
}
```

---

### 3. 攻略管理

#### 3.1 获取攻略列表

**请求**

```
GET /api/admin/v1/guides?page=1&pageSize=10&keyword=北京&published=1
```

**响应**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "list": [
      {
        "id": 1,
        "title": "北京三日游攻略",
        "coverImage": "https://example.com/guide1.jpg",
        "category": "行程规划",
        "viewCount": 1234,
        "published": true,
        "createdAt": "2025-01-01 10:00:00"
      }
    ],
    "total": 50,
    "page": 1,
    "pageSize": 10,
    "totalPages": 5
  }
}
```

#### 3.2 获取攻略详情

**请求**

```
GET /api/admin/v1/guides/{guideId}
```

**响应**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": 1,
    "title": "北京三日游攻略",
    "content": "<p>北京是中国的首都...</p>",
    "coverImage": "https://example.com/guide1.jpg",
    "category": "行程规划",
    "relatedSpotIds": [101, 102],
    "published": true,
    "createdAt": "2025-01-01 10:00:00",
    "updatedAt": "2025-01-01 10:00:00"
  }
}
```

#### 3.3 创建攻略

**请求**

```
POST /api/admin/v1/guides
```

**请求体**

```json
{
  "title": "北京三日游攻略",
  "content": "<p>北京是中国的首都...</p>",
  "coverImage": "https://example.com/guide1.jpg",
  "category": "行程规划",
  "relatedSpotIds": [101, 102],
  "published": false
}
```

**响应**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": 1
  }
}
```

#### 3.4 更新攻略

**请求**

```
PUT /api/admin/v1/guides/{guideId}
```

**请求体**

同创建攻略，所有字段可选

**响应**

```json
{
  "code": 0,
  "message": "success",
  "data": null
}
```

#### 3.5 更新发布状态

**请求**

```
PUT /api/admin/v1/guides/{guideId}/publish
```

**请求体**

```json
{
  "published": true
}
```

**响应**

```json
{
  "code": 0,
  "message": "success",
  "data": null
}
```

#### 3.6 删除攻略

**请求**

```
DELETE /api/admin/v1/guides/{guideId}
```

**响应**

```json
{
  "code": 0,
  "message": "success",
  "data": null
}
```

#### 3.7 获取攻略分类列表

**请求**

```
GET /api/admin/v1/guides/categories
```

**响应**

```json
{
  "code": 0,
  "message": "success",
  "data": ["行程规划", "美食推荐", "住宿攻略", "交通指南"]
}
```

---

### 4. 订单管理

#### 4.1 获取订单列表

**请求**

```
GET /api/admin/v1/orders?page=1&pageSize=10&status=1&startDate=2025-01-01&endDate=2025-01-31
```

| 参数      | 类型   | 必填 | 说明                                                   |
| --------- | ------ | ---- | ------------------------------------------------------ |
| page      | int    | 否   | 页码，默认1                                            |
| pageSize  | int    | 否   | 每页数量，默认10                                       |
| status    | int    | 否   | 状态筛选：0-待支付/1-已支付/2-已取消/3-已退款/4-已完成 |
| startDate | string | 否   | 开始日期                                               |
| endDate   | string | 否   | 结束日期                                               |
| orderNo   | string | 否   | 订单号搜索                                             |

**响应**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "list": [
      {
        "id": 1,
        "orderNo": "ORD20250101100000001",
        "userId": 1,
        "userNickname": "用户A",
        "spotName": "故宫博物院",
        "totalPrice": 120.0,
        "quantity": 2,
        "visitDate": "2025-02-01",
        "status": "PAID",
        "statusText": "已支付",
        "createdAt": "2025-01-01 10:00:00"
      }
    ],
    "total": 100,
    "page": 1,
    "pageSize": 10,
    "totalPages": 10
  }
}
```

#### 4.2 获取订单详情

**请求**

```
GET /api/admin/v1/orders/{orderId}
```

**响应**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": 1,
    "orderNo": "ORD20250101100000001",
    "userId": 1,
    "userNickname": "用户A",
    "spotId": 101,
    "spotName": "故宫博物院",
    "spotImage": "https://example.com/spot1.jpg",
    "unitPrice": 60.0,
    "quantity": 2,
    "totalPrice": 120.0,
    "visitDate": "2025-02-01",
    "contactName": "张三",
    "contactPhone": "13800138000",
    "status": "PAID",
    "statusText": "已支付",
    "createdAt": "2025-01-01 10:00:00",
    "paidAt": "2025-01-01 10:05:00",
    "cancelledAt": null,
    "refundedAt": null,
    "completedAt": null
  }
}
```

#### 4.3 完成订单

将已支付订单标记为已完成。

**请求**

```
POST /api/admin/v1/orders/{orderId}/complete
```

**响应**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "orderId": 1,
    "status": "COMPLETED",
    "statusText": "已完成",
    "completedAt": "2025-02-01 18:00:00"
  }
}
```

#### 4.4 退款订单

将已支付订单退款。

**请求**

```
POST /api/admin/v1/orders/{orderId}/refund
```

**响应**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "orderId": 1,
    "status": "REFUNDED",
    "statusText": "已退款",
    "refundedAt": "2025-02-01 18:00:00"
  }
}
```

---

### 5. 用户管理

#### 5.1 获取用户列表

**请求**

```
GET /api/admin/v1/users?page=1&pageSize=10&keyword=用户
```

| 参数      | 类型   | 必填 | 说明             |
| --------- | ------ | ---- | ---------------- |
| page      | int    | 否   | 页码，默认1      |
| pageSize  | int    | 否   | 每页数量，默认10 |
| keyword   | string | 否   | 昵称搜索         |
| startDate | string | 否   | 注册开始日期     |
| endDate   | string | 否   | 注册结束日期     |

**响应**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "list": [
      {
        "id": 1,
        "nickname": "用户A",
        "avatar": "https://example.com/avatar1.jpg",
        "orderCount": 5,
        "ratingCount": 10,
        "favoriteCount": 20,
        "createdAt": "2025-01-01 10:00:00"
      }
    ],
    "total": 1000,
    "page": 1,
    "pageSize": 10,
    "totalPages": 100
  }
}
```

注意：响应中不包含 OpenID 等敏感信息

#### 5.2 获取用户详情

**请求**

```
GET /api/admin/v1/users/{userId}
```

**响应**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": 1,
    "nickname": "用户A",
    "avatar": "https://example.com/avatar1.jpg",
    "preferences": ["自然风光", "历史文化"],
    "orderCount": 5,
    "ratingCount": 10,
    "favoriteCount": 20,
    "createdAt": "2025-01-01 10:00:00",
    "updatedAt": "2025-01-05 15:00:00"
  }
}
```

---

### 6. 轮播图管理

#### 6.1 获取轮播图列表

**请求**

```
GET /api/admin/v1/banners
```

**响应**

```json
{
  "code": 0,
  "message": "success",
  "data": [
    {
      "id": 1,
      "imageUrl": "https://example.com/banner1.jpg",
      "spotId": 101,
      "spotName": "故宫博物院",
      "sortOrder": 1,
      "enabled": true,
      "createdAt": "2025-01-01 10:00:00"
    }
  ]
}
```

#### 6.2 创建轮播图

**请求**

```
POST /api/admin/v1/banners
```

**请求体**

```json
{
  "imageUrl": "https://example.com/banner1.jpg",
  "spotId": 101,
  "sortOrder": 1,
  "enabled": true
}
```

**响应**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": 1
  }
}
```

#### 6.3 更新轮播图

**请求**

```
PUT /api/admin/v1/banners/{bannerId}
```

**请求体**

```json
{
  "imageUrl": "https://example.com/banner1-new.jpg",
  "spotId": 102,
  "sortOrder": 2,
  "enabled": true
}
```

**响应**

```json
{
  "code": 0,
  "message": "success",
  "data": null
}
```

#### 6.4 删除轮播图

**请求**

```
DELETE /api/admin/v1/banners/{bannerId}
```

**响应**

```json
{
  "code": 0,
  "message": "success",
  "data": null
}
```

#### 6.5 切换启用状态

**请求**

```
POST /api/admin/v1/banners/{bannerId}/toggle
```

**响应**

```json
{
  "code": 0,
  "message": "success",
  "data": null
}
```

---

### 7. 数据统计（仪表板）

#### 7.1 获取概览数据

**请求**

```
GET /api/admin/v1/dashboard/overview
```

**响应**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "totalUsers": 10000,
    "totalSpots": 500,
    "totalOrders": 50000,
    "totalRevenue": 3000000.0,
    "todayNewUsers": 50,
    "todayOrders": 200,
    "todayRevenue": 12000.0
  }
}
```

#### 7.2 获取订单趋势

**请求**

```
GET /api/admin/v1/dashboard/order-trend?days=7
```

**响应**

```json
{
  "code": 0,
  "message": "success",
  "data": [
    { "date": "2025-01-01", "orderCount": 150, "revenue": 9000.0 },
    { "date": "2025-01-02", "orderCount": 180, "revenue": 10800.0 }
  ]
}
```

#### 7.3 获取热门景点排行

**请求**

```
GET /api/admin/v1/dashboard/hot-spots?limit=10
```

**响应**

```json
{
  "code": 0,
  "message": "success",
  "data": [
    {
      "spotId": 101,
      "spotName": "故宫博物院",
      "orderCount": 500,
      "revenue": 30000.0
    }
  ]
}
```

---

## 文件上传

### 上传图片

**请求**

```
POST /api/admin/v1/upload/image
Content-Type: multipart/form-data
```

| 参数 | 类型 | 必填 | 说明                                        |
| ---- | ---- | ---- | ------------------------------------------- |
| file | file | 是   | 图片文件（支持 jpg/png/gif/webp，最大 5MB） |

**响应**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "url": "https://example.com/uploads/2025/01/01/xxx.jpg"
  }
}
```

---

## 接口汇总

### 用户端接口（共 27 个）

| 模块 | 接口           | 方法   | 路径                                   |
| ---- | -------------- | ------ | -------------------------------------- |
| 认证 | 微信登录       | POST   | /api/v1/auth/wx-login                  |
| 认证 | 获取用户信息   | GET    | /api/v1/auth/user-info                 |
| 认证 | 更新用户信息   | PUT    | /api/v1/auth/user-info                 |
| 认证 | 设置偏好标签   | POST   | /api/v1/auth/preferences               |
| 首页 | 获取轮播图     | GET    | /api/v1/home/banners                   |
| 首页 | 获取热门推荐   | GET    | /api/v1/home/hot                       |
| 推荐 | 获取个性化推荐 | GET    | /api/v1/recommendations                |
| 推荐 | 刷新推荐       | POST   | /api/v1/recommendations/refresh        |
| 景点 | 获取景点列表   | GET    | /api/v1/spots                          |
| 景点 | 搜索景点       | GET    | /api/v1/spots/search                   |
| 景点 | 获取景点详情   | GET    | /api/v1/spots/{spotId}                 |
| 景点 | 获取筛选选项   | GET    | /api/v1/spots/filters                  |
| 攻略 | 获取攻略列表   | GET    | /api/v1/guides                         |
| 攻略 | 获取攻略详情   | GET    | /api/v1/guides/{guideId}               |
| 攻略 | 获取攻略分类   | GET    | /api/v1/guides/categories              |
| 评分 | 提交评分       | POST   | /api/v1/ratings                        |
| 评分 | 获取用户评分   | GET    | /api/v1/ratings/spot/{spotId}          |
| 评分 | 获取评论列表   | GET    | /api/v1/ratings/spot/{spotId}/comments |
| 收藏 | 添加收藏       | POST   | /api/v1/favorites                      |
| 收藏 | 取消收藏       | DELETE | /api/v1/favorites/{spotId}             |
| 收藏 | 获取收藏列表   | GET    | /api/v1/favorites                      |
| 收藏 | 检查收藏状态   | GET    | /api/v1/favorites/check/{spotId}       |
| 订单 | 创建订单       | POST   | /api/v1/orders                         |
| 订单 | 获取订单列表   | GET    | /api/v1/orders                         |
| 订单 | 获取订单详情   | GET    | /api/v1/orders/{orderId}               |
| 订单 | 模拟支付       | POST   | /api/v1/orders/{orderId}/pay           |
| 订单 | 取消订单       | POST   | /api/v1/orders/{orderId}/cancel        |

### 管理端接口（共 30 个）

| 模块   | 接口           | 方法   | 路径                                    |
| ------ | -------------- | ------ | --------------------------------------- |
| 认证   | 管理员登录     | POST   | /api/admin/v1/auth/login                |
| 认证   | 获取管理员信息 | GET    | /api/admin/v1/auth/info                 |
| 景点   | 获取景点列表   | GET    | /api/admin/v1/spots                     |
| 景点   | 获取景点详情   | GET    | /api/admin/v1/spots/{spotId}            |
| 景点   | 创建景点       | POST   | /api/admin/v1/spots                     |
| 景点   | 更新景点       | PUT    | /api/admin/v1/spots/{spotId}            |
| 景点   | 更新发布状态   | PUT    | /api/admin/v1/spots/{spotId}/publish    |
| 景点   | 删除景点       | DELETE | /api/admin/v1/spots/{spotId}            |
| 攻略   | 获取攻略列表   | GET    | /api/admin/v1/guides                    |
| 攻略   | 获取攻略详情   | GET    | /api/admin/v1/guides/{guideId}          |
| 攻略   | 获取攻略分类   | GET    | /api/admin/v1/guides/categories         |
| 攻略   | 创建攻略       | POST   | /api/admin/v1/guides                    |
| 攻略   | 更新攻略       | PUT    | /api/admin/v1/guides/{guideId}          |
| 攻略   | 更新发布状态   | PUT    | /api/admin/v1/guides/{guideId}/publish  |
| 攻略   | 删除攻略       | DELETE | /api/admin/v1/guides/{guideId}          |
| 订单   | 获取订单列表   | GET    | /api/admin/v1/orders                    |
| 订单   | 获取订单详情   | GET    | /api/admin/v1/orders/{orderId}          |
| 订单   | 完成订单       | POST   | /api/admin/v1/orders/{orderId}/complete |
| 订单   | 退款订单       | POST   | /api/admin/v1/orders/{orderId}/refund   |
| 用户   | 获取用户列表   | GET    | /api/admin/v1/users                     |
| 用户   | 获取用户详情   | GET    | /api/admin/v1/users/{userId}            |
| 轮播图 | 获取轮播图列表 | GET    | /api/admin/v1/banners                   |
| 轮播图 | 创建轮播图     | POST   | /api/admin/v1/banners                   |
| 轮播图 | 更新轮播图     | PUT    | /api/admin/v1/banners/{bannerId}        |
| 轮播图 | 删除轮播图     | DELETE | /api/admin/v1/banners/{bannerId}        |
| 轮播图 | 切换启用状态   | POST   | /api/admin/v1/banners/{bannerId}/toggle |
| 统计   | 获取概览数据   | GET    | /api/admin/v1/dashboard/overview        |
| 统计   | 获取订单趋势   | GET    | /api/admin/v1/dashboard/order-trend     |
| 统计   | 获取热门景点   | GET    | /api/admin/v1/dashboard/hot-spots       |
| 上传   | 上传图片       | POST   | /api/admin/v1/upload/image              |

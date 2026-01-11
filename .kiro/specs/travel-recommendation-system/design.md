# 设计文档

## 概述

本设计文档描述了基于协同过滤推荐算法的个性化旅游推荐系统的技术架构和实现方案。系统采用前后端分离架构，包含基于 Uni-app 的跨端小程序（用户端）和 Web 管理后台（管理端），后端使用 Java 17 + Spring Boot 3.2.12 + MyBatis-Plus 构建 RESTful API，数据库使用 MySQL 8.0，推荐算法采用 ItemCF（基于物品的协同过滤）。

## 架构

### 整体架构图

```mermaid
graph TB
    subgraph 用户端
        MP[Uni-app 小程序<br/>Vue.js 3 + Pinia]
    end
    
    subgraph 管理端
        WEB[Web 管理后台<br/>Vue.js 3 + Element Plus]
    end
    
    subgraph 后端服务
        GW[API 网关]
        AUTH[认证服务]
        SPOT[景点服务]
        GUIDE[攻略服务]
        ORDER[订单服务]
        RATING[评分服务]
        FAV[收藏服务]
        REC[推荐引擎]
    end
    
    subgraph 数据层
        DB[(MySQL)]
        CACHE[(Redis 缓存)]
    end
    
    MP --> GW
    WEB --> GW
    GW --> AUTH
    GW --> SPOT
    GW --> GUIDE
    GW --> ORDER
    GW --> RATING
    GW --> FAV
    GW --> REC
    
    AUTH --> DB
    SPOT --> DB
    GUIDE --> DB
    ORDER --> DB
    RATING --> DB
    FAV --> DB
    REC --> DB
    REC --> CACHE
```

### 技术栈

| 层级 | 技术选型 |
|------|----------|
| 用户端 | Uni-app + Vue.js 3 + Pinia |
| 管理端 | Vue.js 3 + Element Plus + Axios |
| 后端 | Java 17 + Spring Boot 3.2.12 + MyBatis-Plus 3.5.5 |
| 数据库 | MySQL 8.0 |
| 缓存 | Redis |
| 认证 | JWT Token |
| API 文档 | Knife4j 4.5.0 (OpenAPI 3) |

### 部署架构

```mermaid
graph LR
    subgraph 微信云
        MP[Uni-app 小程序]
    end
    
    subgraph 云服务器
        NGINX[Nginx]
        APP[Spring Boot 3.2.12 App]
        ADMIN[Vue Admin]
    end
    
    subgraph 数据服务
        MYSQL[(MySQL)]
        REDIS[(Redis)]
    end
    
    MP --> NGINX
    NGINX --> APP
    NGINX --> ADMIN
    APP --> MYSQL
    APP --> REDIS
```

## 组件和接口

### 1. 认证服务 (AuthService)

负责用户登录、注册和身份验证。

```java
public interface AuthService {
    /**
     * 微信小程序登录
     * @param code 微信登录凭证
     * @return 登录结果，包含 token 和用户信息
     */
    LoginResult wxLogin(String code);
    
    /**
     * 管理员登录
     * @param username 用户名
     * @param password 密码
     * @return 登录结果，包含 token
     */
    LoginResult adminLogin(String username, String password);
    
    /**
     * 更新用户信息
     * @param userId 用户ID
     * @param nickname 昵称
     * @param avatar 头像URL
     */
    void updateUserInfo(Long userId, String nickname, String avatar);
    
    /**
     * 验证 Token
     * @param token JWT token
     * @return 用户信息
     */
    UserInfo validateToken(String token);
}
```

### 2. 景点服务 (SpotService)

管理景点数据的增删改查。

```java
public interface SpotService {
    /**
     * 分页查询景点列表
     * @param query 查询条件（地区、类型、排序方式）
     * @param pageable 分页参数
     * @return 景点分页列表
     */
    Page<SpotDTO> listSpots(SpotQuery query, Pageable pageable);
    
    /**
     * 搜索景点
     * @param keyword 关键词
     * @param pageable 分页参数
     * @return 匹配的景点列表
     */
    Page<SpotDTO> searchSpots(String keyword, Pageable pageable);
    
    /**
     * 获取景点详情
     * @param spotId 景点ID
     * @return 景点详情
     */
    SpotDetailDTO getSpotDetail(Long spotId);
    
    /**
     * 创建景点（管理端）
     * @param spot 景点信息
     * @return 创建的景点
     */
    SpotDTO createSpot(SpotCreateRequest spot);
    
    /**
     * 更新景点（管理端）
     * @param spotId 景点ID
     * @param spot 更新信息
     */
    void updateSpot(Long spotId, SpotUpdateRequest spot);
    
    /**
     * 更新发布状态（管理端）
     * @param spotId 景点ID
     * @param published 是否发布
     */
    void updatePublishStatus(Long spotId, boolean published);
}
```

### 3. 推荐引擎 (RecommendationEngine)

基于 ItemCF 算法的个性化推荐。

```java
public interface RecommendationEngine {
    /**
     * 获取个性化推荐
     * @param userId 用户ID
     * @param topN 推荐数量
     * @return 推荐景点列表
     */
    List<SpotDTO> getPersonalizedRecommendations(Long userId, int topN);
    
    /**
     * 获取热门推荐（冷启动）
     * @param topN 推荐数量
     * @return 热门景点列表
     */
    List<SpotDTO> getHotRecommendations(int topN);
    
    /**
     * 判断用户是否满足个性化推荐条件
     * @param userId 用户ID
     * @return 是否满足（评分数量 >= 3）
     */
    boolean hasEnoughInteractions(Long userId);
    
    /**
     * 计算物品相似度矩阵（定时任务）
     */
    void computeItemSimilarityMatrix();
    
    /**
     * 刷新推荐结果
     * @param userId 用户ID
     * @param topN 推荐数量
     * @return 新一批推荐结果
     */
    List<SpotDTO> refreshRecommendations(Long userId, int topN);
}
```

### 4. 攻略服务 (GuideService)

管理旅游攻略内容。

```java
public interface GuideService {
    /**
     * 分页查询攻略列表
     * @param category 分类（可选）
     * @param sortBy 排序方式（time/category）
     * @param pageable 分页参数
     * @return 攻略分页列表
     */
    Page<GuideDTO> listGuides(String category, String sortBy, Pageable pageable);
    
    /**
     * 获取攻略详情
     * @param guideId 攻略ID
     * @return 攻略详情（含富文本）
     */
    GuideDetailDTO getGuideDetail(Long guideId);
    
    /**
     * 创建攻略（管理端）
     * @param guide 攻略信息
     * @return 创建的攻略
     */
    GuideDTO createGuide(GuideCreateRequest guide);
    
    /**
     * 更新攻略（管理端）
     * @param guideId 攻略ID
     * @param guide 更新信息
     */
    void updateGuide(Long guideId, GuideUpdateRequest guide);
    
    /**
     * 更新发布状态（管理端）
     * @param guideId 攻略ID
     * @param published 是否发布
     */
    void updatePublishStatus(Long guideId, boolean published);
}
```

### 5. 订单服务 (OrderService)

处理订单创建、支付、取消等流程。

```java
public interface OrderService {
    /**
     * 创建订单
     * @param userId 用户ID
     * @param request 订单创建请求
     * @return 创建的订单
     */
    OrderDTO createOrder(Long userId, OrderCreateRequest request);
    
    /**
     * 分页查询用户订单
     * @param userId 用户ID
     * @param status 订单状态（可选）
     * @param pageable 分页参数
     * @return 订单分页列表
     */
    Page<OrderDTO> listUserOrders(Long userId, OrderStatus status, Pageable pageable);
    
    /**
     * 获取订单详情
     * @param orderId 订单ID
     * @return 订单详情
     */
    OrderDetailDTO getOrderDetail(Long orderId);
    
    /**
     * 模拟支付
     * @param orderId 订单ID
     * @return 支付结果
     */
    PaymentResult simulatePayment(Long orderId);
    
    /**
     * 取消订单
     * @param orderId 订单ID
     */
    void cancelOrder(Long orderId);
    
    /**
     * 完成订单（管理端）
     * @param orderId 订单ID
     */
    void completeOrder(Long orderId);
    
    /**
     * 分页查询所有订单（管理端）
     * @param query 查询条件
     * @param pageable 分页参数
     * @return 订单分页列表
     */
    Page<OrderDTO> listAllOrders(OrderQuery query, Pageable pageable);
}
```

### 6. 评分服务 (RatingService)

处理用户对景点的评分与评论。

```java
public interface RatingService {
    /**
     * 提交评分
     * @param userId 用户ID
     * @param spotId 景点ID
     * @param score 评分（1-5）
     * @param comment 评论（可选）
     */
    void submitRating(Long userId, Long spotId, int score, String comment);
    
    /**
     * 获取用户对景点的评分
     * @param userId 用户ID
     * @param spotId 景点ID
     * @return 评分信息
     */
    RatingDTO getUserRating(Long userId, Long spotId);
    
    /**
     * 获取景点的评论列表
     * @param spotId 景点ID
     * @param pageable 分页参数
     * @return 评论分页列表
     */
    Page<RatingDTO> getSpotRatings(Long spotId, Pageable pageable);
    
    /**
     * 获取用户评分数量
     * @param userId 用户ID
     * @return 评分数量
     */
    int getUserRatingCount(Long userId);
}
```

### 7. 收藏服务 (FavoriteService)

管理用户收藏列表。

```java
public interface FavoriteService {
    /**
     * 添加收藏
     * @param userId 用户ID
     * @param spotId 景点ID
     */
    void addFavorite(Long userId, Long spotId);
    
    /**
     * 取消收藏
     * @param userId 用户ID
     * @param spotId 景点ID
     */
    void removeFavorite(Long userId, Long spotId);
    
    /**
     * 检查是否已收藏
     * @param userId 用户ID
     * @param spotId 景点ID
     * @return 是否已收藏
     */
    boolean isFavorite(Long userId, Long spotId);
    
    /**
     * 获取用户收藏列表
     * @param userId 用户ID
     * @param pageable 分页参数
     * @return 收藏的景点列表
     */
    Page<SpotDTO> listFavorites(Long userId, Pageable pageable);
}
```



## 数据模型

### 实体关系图

```mermaid
erDiagram
    USER ||--o{ ORDER : creates
    USER ||--o{ RATING : submits
    USER ||--o{ FAVORITE : has
    USER ||--o{ USER_PREFERENCE : has
    
    SPOT ||--o{ ORDER : contains
    SPOT ||--o{ RATING : receives
    SPOT ||--o{ FAVORITE : in
    SPOT }o--|| SPOT_CATEGORY : belongs_to
    SPOT }o--|| REGION : located_in
    
    GUIDE ||--o{ GUIDE_SPOT : references
    SPOT ||--o{ GUIDE_SPOT : referenced_by
    
    ADMIN ||--o{ GUIDE : creates
    
    BANNER ||--|| SPOT : links_to

    USER {
        bigint id PK
        varchar openid UK
        varchar nickname
        varchar avatar
        datetime created_at
        datetime updated_at
    }
    
    USER_PREFERENCE {
        bigint id PK
        bigint user_id FK
        varchar tag
        datetime created_at
    }
    
    ADMIN {
        bigint id PK
        varchar username UK
        varchar password_hash
        varchar salt
        datetime created_at
        datetime last_login
    }
    
    SPOT {
        bigint id PK
        varchar name
        text description
        decimal price
        varchar open_time
        varchar address
        decimal latitude
        decimal longitude
        varchar images
        bigint category_id FK
        bigint region_id FK
        int heat_score
        decimal avg_rating
        boolean published
        datetime created_at
        datetime updated_at
    }
    
    SPOT_CATEGORY {
        bigint id PK
        varchar name
        int sort_order
    }
    
    REGION {
        bigint id PK
        varchar name
        int sort_order
    }
    
    GUIDE {
        bigint id PK
        varchar title
        text content
        varchar cover_image
        varchar category
        bigint admin_id FK
        boolean published
        datetime created_at
        datetime updated_at
    }
    
    GUIDE_SPOT {
        bigint id PK
        bigint guide_id FK
        bigint spot_id FK
    }
    
    ORDER {
        bigint id PK
        varchar order_no UK
        bigint user_id FK
        bigint spot_id FK
        date visit_date
        int quantity
        decimal total_price
        varchar contact_name
        varchar contact_phone
        varchar status
        datetime created_at
        datetime updated_at
    }
    
    RATING {
        bigint id PK
        bigint user_id FK
        bigint spot_id FK
        int score
        text comment
        datetime created_at
        datetime updated_at
    }
    
    FAVORITE {
        bigint id PK
        bigint user_id FK
        bigint spot_id FK
        datetime created_at
    }
    
    BANNER {
        bigint id PK
        varchar image_url
        bigint spot_id FK
        int sort_order
        boolean enabled
        datetime created_at
    }
```

### 核心数据结构

#### 订单状态枚举

```java
public enum OrderStatus {
    PENDING_PAYMENT("待支付"),
    PENDING_USE("待使用"),
    COMPLETED("已完成"),
    CANCELLED("已取消");
    
    private final String description;
}
```

#### 物品相似度矩阵（ItemCF）

```java
/**
 * 物品相似度矩阵存储结构
 * 使用 Redis Hash 存储，key 格式: item_similarity:{spotId}
 * field: 相似景点ID, value: 相似度分数
 */
public class ItemSimilarityMatrix {
    private Map<Long, Map<Long, Double>> similarityMatrix;
    
    /**
     * 获取与指定景点最相似的 Top-K 景点
     * @param spotId 景点ID
     * @param k 数量
     * @return 相似景点及其相似度
     */
    public List<SimilarItem> getTopKSimilar(Long spotId, int k);
}

public class SimilarItem {
    private Long spotId;
    private Double similarity;
}
```

### ItemCF 推荐算法流程

```mermaid
flowchart TD
    A[开始推荐] --> B{用户评分数 >= 3?}
    B -->|是| C[获取用户评分过的景点]
    B -->|否| D[冷启动策略]
    
    C --> E[查询相似度矩阵]
    E --> F[计算候选景点得分]
    F --> G[过滤已交互景点]
    G --> H[排序取 Top-N]
    H --> I[返回推荐结果]
    
    D --> J[返回热门/最新景点]
    J --> K[引导选择偏好标签]
    K --> I
    
    subgraph 过滤规则
        G1[已评分景点]
        G2[已收藏景点]
        G3[已下单景点<br/>不含已取消]
    end
    
    G --> G1
    G --> G2
    G --> G3
```

### 推荐得分计算公式

对于用户 u，候选景点 j 的推荐得分计算如下：

```
Score(u, j) = Σ (sim(i, j) × rating(u, i)) / Σ sim(i, j)

其中：
- i 为用户 u 评分过的景点
- sim(i, j) 为景点 i 和 j 的相似度
- rating(u, i) 为用户 u 对景点 i 的评分
```

### 物品相似度计算（余弦相似度）

```
sim(i, j) = |U(i) ∩ U(j)| / sqrt(|U(i)| × |U(j)|)

其中：
- U(i) 为评分过景点 i 的用户集合
- U(j) 为评分过景点 j 的用户集合
```



## 正确性属性

*正确性属性是系统在所有有效执行中都应保持为真的特征或行为——本质上是关于系统应该做什么的形式化陈述。属性作为人类可读规范和机器可验证正确性保证之间的桥梁。*

### Property 1: 登录幂等性

*对于任意* OpenID，无论登录多少次，系统应返回相同的用户记录，不创建重复用户。

**验证: 需求 1.2, 1.3**

### Property 2: 用户信息更新往返

*对于任意* 用户和任意有效昵称，更新昵称后立即查询应返回更新后的值。

**验证: 需求 1.5**

### Property 3: 热门推荐排序

*对于任意* 热门推荐列表，列表中的景点应按热度分数降序排列。

**验证: 需求 2.3**

### Property 4: 推荐过滤规则

*对于任意* 有足够交互记录的用户，推荐结果不应包含该用户已评分、已收藏或已下单（不含已取消订单）的景点。

**验证: 需求 3.3, 3.4**

### Property 5: 冷启动策略

*对于任意* 评分数量少于3的用户，推荐引擎应返回热门景点列表而非个性化推荐。

**验证: 需求 3.2**

### Property 6: 个性化推荐差异性

*对于任意* 有足够交互记录的用户，其个性化推荐列表应与全局热门列表存在差异（至少有一个不同的景点）。

**验证: 需求 3.6**

### Property 7: 景点筛选正确性

*对于任意* 景点列表查询，当指定地区或类型筛选条件时，返回的所有景点应满足该筛选条件。

**验证: 需求 4.1**

### Property 8: 景点排序正确性

*对于任意* 景点列表查询，当指定排序方式（热度/评分/价格）时，返回的景点应按该字段正确排序。

**验证: 需求 4.2**

### Property 9: 搜索结果相关性

*对于任意* 搜索关键词，返回的所有景点名称应包含该关键词（模糊匹配）。

**验证: 需求 4.4**

### Property 10: 景点详情完整性

*对于任意* 景点详情查询，返回的数据应包含图片、简介、票价、开放时间、地址和经纬度。

**验证: 需求 4.6**

### Property 11: 攻略排序正确性

*对于任意* 攻略列表查询，当指定排序方式时，返回的攻略应按该方式正确排序。

**验证: 需求 5.1**

### Property 12: 发布状态过滤

*对于任意* 用户端的景点或攻略列表查询，返回的所有项目应为已发布状态（published=true）。

**验证: 需求 5.4, 14.3, 15.3**

### Property 13: 评分持久化往返

*对于任意* 用户、景点和有效评分（1-5），提交评分后查询应返回相同的评分值。

**验证: 需求 6.1**

### Property 14: 评分更新幂等性

*对于任意* 用户和景点，多次提交评分应更新现有记录而非创建新记录，最终只有一条评分记录。

**验证: 需求 6.2**

### Property 15: 评论持久化往返

*对于任意* 用户、景点和评论内容，提交评论后在景点详情页应能查询到该评论。

**验证: 需求 6.3**

### Property 16: 收藏操作往返

*对于任意* 用户和景点，添加收藏后查询收藏列表应包含该景点；移除收藏后查询收藏列表不应包含该景点。

**验证: 需求 7.1, 7.2, 7.3**

### Property 17: 订单创建状态

*对于任意* 有效的订单创建请求，创建的订单初始状态应为"待支付"。

**验证: 需求 8.1**

### Property 18: 订单状态筛选

*对于任意* 订单列表查询，当指定状态筛选条件时，返回的所有订单应为该状态。

**验证: 需求 8.2**

### Property 19: 支付状态转换

*对于任意* 状态为"待支付"的订单，成功支付后状态应变为"待使用"。

**验证: 需求 9.2**

### Property 20: 支付幂等性

*对于任意* 订单，多次调用支付接口应产生相同结果，不产生重复支付。

**验证: 需求 9.3**

### Property 21: 订单状态持久化

*对于任意* 订单状态变更，变更后刷新页面应显示新状态，不回退到旧状态。

**验证: 需求 9.4**

### Property 22: 订单状态机约束

*对于任意* 订单：
- 只有"待支付"或"待使用"状态的订单可以被取消
- "已完成"状态的订单不能被取消
- "已完成"状态不能回退到"待使用"

**验证: 需求 10.1, 10.3, 11.3**

### Property 23: 取消订单幂等性

*对于任意* 已取消的订单，再次调用取消接口应幂等处理，不报错。

**验证: 需求 10.4**

### Property 24: 订单完成状态转换

*对于任意* 状态为"待使用"的订单，管理员标记完成后状态应变为"已完成"。

**验证: 需求 11.1**

### Property 25: 访问控制

*对于任意* 未携带有效 token 的 API 请求，系统应拒绝访问并返回认证错误。

**验证: 需求 13.2, 18.4**

### Property 26: 密码安全存储

*对于任意* 管理员账户，数据库中存储的密码应为 hash + salt 加密形式，不应为明文。

**验证: 需求 13.3**

### Property 27: 景点数据持久化往返

*对于任意* 有效的景点创建请求，创建后查询应返回相同的景点数据。

**验证: 需求 14.1, 14.2**

### Property 28: 攻略数据持久化往返

*对于任意* 有效的攻略创建请求，创建后查询应返回相同的攻略数据。

**验证: 需求 15.1, 15.2**

### Property 29: 订单筛选正确性（管理端）

*对于任意* 管理端订单列表查询，当指定时间或状态筛选条件时，返回的所有订单应满足该条件。

**验证: 需求 16.1**

### Property 30: 用户信息脱敏

*对于任意* 管理端用户列表查询，返回的数据不应包含 OpenID 等敏感标识符。

**验证: 需求 17.2**

### Property 31: 订单操作幂等性

*对于任意* 订单创建请求，在网络重试场景下，相同请求不应创建重复订单。

**验证: 需求 18.6**



## 错误处理

### 错误码设计

| 错误码 | 描述 | HTTP 状态码 |
|--------|------|-------------|
| 10001 | 微信登录失败 | 401 |
| 10002 | Token 无效或过期 | 401 |
| 10003 | 无权限访问 | 403 |
| 20001 | 景点不存在 | 404 |
| 20002 | 景点已下架 | 400 |
| 30001 | 攻略不存在 | 404 |
| 30002 | 攻略已下架 | 400 |
| 40001 | 订单不存在 | 404 |
| 40002 | 订单状态不允许此操作 | 400 |
| 40003 | 订单已支付 | 400 |
| 40004 | 订单已取消 | 400 |
| 50001 | 评分值无效（需1-5） | 400 |
| 60001 | 参数校验失败 | 400 |
| 60002 | 服务器内部错误 | 500 |

### 统一响应格式

```java
public class ApiResponse<T> {
    private int code;        // 业务状态码，0表示成功
    private String message;  // 提示信息
    private T data;          // 响应数据
    private long timestamp;  // 时间戳
}
```

### 异常处理策略

```mermaid
flowchart TD
    A[请求进入] --> B{参数校验}
    B -->|失败| C[返回 60001]
    B -->|成功| D{业务处理}
    D -->|业务异常| E[返回业务错误码]
    D -->|系统异常| F[记录日志]
    F --> G[返回 60002]
    D -->|成功| H[返回成功响应]
```

### 重试机制

对于订单相关操作，采用幂等性设计：

1. **订单创建**: 使用客户端生成的请求ID作为幂等键
2. **支付操作**: 基于订单号的幂等处理
3. **状态变更**: 乐观锁 + 状态机校验

```java
@Transactional
public PaymentResult simulatePayment(Long orderId) {
    Order order = orderRepository.findByIdWithLock(orderId);
    
    // 幂等检查：已支付直接返回成功
    if (order.getStatus() == OrderStatus.PENDING_USE) {
        return PaymentResult.alreadyPaid();
    }
    
    // 状态校验
    if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
        throw new BusinessException(40002, "订单状态不允许支付");
    }
    
    // 更新状态
    order.setStatus(OrderStatus.PENDING_USE);
    orderRepository.save(order);
    
    return PaymentResult.success();
}
```

## 测试策略

### 测试分层

```mermaid
pyramid
    title 测试金字塔
    "E2E 测试" : 10
    "集成测试" : 30
    "单元测试" : 60
```

### 单元测试

- **覆盖范围**: 所有 Service 层业务逻辑
- **框架**: JUnit 5 + Mockito
- **重点场景**:
  - 推荐算法的过滤逻辑
  - 订单状态机转换
  - 评分计算逻辑

### 属性测试

- **框架**: jqwik (Java Property-Based Testing)
- **配置**: 每个属性测试至少运行 100 次迭代
- **标注格式**: `@Property // Feature: travel-recommendation-system, Property N: {property_text}`

#### 属性测试示例

```java
@Property(tries = 100)
// Feature: travel-recommendation-system, Property 1: 登录幂等性
void loginShouldBeIdempotent(@ForAll @StringLength(min = 28, max = 28) String openId) {
    // 第一次登录
    LoginResult first = authService.wxLogin(mockCode(openId));
    // 第二次登录
    LoginResult second = authService.wxLogin(mockCode(openId));
    
    // 应返回相同用户
    assertThat(first.getUserId()).isEqualTo(second.getUserId());
    // 数据库中只有一条记录
    assertThat(userRepository.countByOpenId(openId)).isEqualTo(1);
}

@Property(tries = 100)
// Feature: travel-recommendation-system, Property 4: 推荐过滤规则
void recommendationsShouldExcludeInteractedSpots(
    @ForAll @LongRange(min = 1, max = 1000) Long userId,
    @ForAll @IntRange(min = 1, max = 20) int topN
) {
    // 假设用户有足够交互记录
    assumeThat(recommendationEngine.hasEnoughInteractions(userId)).isTrue();
    
    // 获取用户已交互的景点
    Set<Long> ratedSpots = ratingService.getUserRatedSpotIds(userId);
    Set<Long> favoriteSpots = favoriteService.getUserFavoriteSpotIds(userId);
    Set<Long> orderedSpots = orderService.getUserOrderedSpotIds(userId, false); // 不含已取消
    
    Set<Long> excludedSpots = new HashSet<>();
    excludedSpots.addAll(ratedSpots);
    excludedSpots.addAll(favoriteSpots);
    excludedSpots.addAll(orderedSpots);
    
    // 获取推荐结果
    List<SpotDTO> recommendations = recommendationEngine.getPersonalizedRecommendations(userId, topN);
    
    // 推荐结果不应包含已交互景点
    for (SpotDTO spot : recommendations) {
        assertThat(excludedSpots).doesNotContain(spot.getId());
    }
}

@Property(tries = 100)
// Feature: travel-recommendation-system, Property 22: 订单状态机约束
void orderStateMachineConstraints(@ForAll OrderStatus currentStatus) {
    Order order = createOrderWithStatus(currentStatus);
    
    // 测试取消操作
    if (currentStatus == OrderStatus.PENDING_PAYMENT || currentStatus == OrderStatus.PENDING_USE) {
        // 应该允许取消
        assertDoesNotThrow(() -> orderService.cancelOrder(order.getId()));
        assertThat(orderRepository.findById(order.getId()).get().getStatus())
            .isEqualTo(OrderStatus.CANCELLED);
    } else if (currentStatus == OrderStatus.COMPLETED) {
        // 不应该允许取消
        assertThrows(BusinessException.class, () -> orderService.cancelOrder(order.getId()));
    }
    
    // 测试完成操作不能回退
    if (currentStatus == OrderStatus.COMPLETED) {
        assertThrows(BusinessException.class, 
            () -> orderService.updateStatus(order.getId(), OrderStatus.PENDING_USE));
    }
}
```

### 集成测试

- **框架**: Spring Boot Test + Testcontainers (Spring Boot 3.2.12)
- **数据库**: 使用 Testcontainers 启动 MySQL 容器
- **ORM**: MyBatis-Plus 集成测试
- **重点场景**:
  - API 端到端流程
  - 数据库事务一致性
  - 缓存与数据库同步

### E2E 测试

- **用户端**: Uni-app 自动化测试（支持微信小程序）
- **管理端**: Cypress 或 Playwright
- **重点场景**:
  - 完整购票流程
  - 推荐刷新交互
  - 管理员操作流程

### 测试数据策略

1. **单元测试**: 使用 Mock 数据和 Faker 生成随机数据
2. **集成测试**: 使用 SQL 脚本初始化测试数据
3. **属性测试**: 使用 jqwik 的 Arbitrary 生成器

```java
@Provide
Arbitrary<SpotCreateRequest> validSpotRequests() {
    return Combinators.combine(
        Arbitraries.strings().alpha().ofMinLength(2).ofMaxLength(50),  // name
        Arbitraries.strings().ofMinLength(10).ofMaxLength(500),        // description
        Arbitraries.doubles().between(0, 9999.99),                      // price
        Arbitraries.doubles().between(-90, 90),                         // latitude
        Arbitraries.doubles().between(-180, 180)                        // longitude
    ).as(SpotCreateRequest::new);
}
```


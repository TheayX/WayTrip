# travel-server

WayTrip 后端服务，基于 Spring Boot 3 + MyBatis-Plus + Redis，提供用户端与管理端接口、旅游内容管理、订单与评价、文件上传，以及基于行为数据的个性化推荐能力。

## 1. 项目说明

`travel-server` 是 WayTrip 的单体后端应用，当前定位是：

- 一个 Spring Boot 单体服务
- 同时承接用户端与管理端接口
- 使用 MySQL 持久化业务数据
- 使用 Redis 存储推荐缓存、相似度缓存与运行状态
- 提供定时任务，周期性刷新景点热度与推荐相似度矩阵

当前代码结构已经按“保守型重构”做过收口，整体原则是：

- 保持单体项目结构，不做微服务拆分
- 保留 `controller / service / mapper / entity / dto / config / task / util` 这类常见顶层目录
- 对过大的实现类按职责拆分，但仍放在现有工程内统一维护

## 2. 技术栈

- Java 17
- Spring Boot 3.5.11
- Spring Web
- Spring Validation
- Spring Data Redis
- Spring AOP
- MyBatis-Plus 3.5.5
- MySQL
- JWT
- SpringDoc / OpenAPI 3
- Hutool
- Lombok
- Maven

## 3. 运行环境要求

启动前建议准备：

- JDK 17
- Maven 3.9+
- MySQL 8.x
- Redis 6.x 或 7.x

## 4. 快速开始

### 4.1 环境变量

项目支持从根目录 `.env` 文件读取敏感配置。

先复制：

```bash
cp .env.example .env
```

或在 Windows PowerShell 中手动创建 `.env`，参考以下字段：

```properties
DB_HOST=localhost
DB_PORT=3306
DB_NAME=waytrip_db
DB_USERNAME=root
DB_PASSWORD=

REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=

JWT_SECRET=

WECHAT_APPID=
WECHAT_SECRET=

UPLOAD_PATH=./uploads
```

### 4.2 数据库初始化

初始化脚本位于：

- [schema.sql](/E:/Year-4/Grad-Project/WayTrip/travel-server/src/main/resources/db/schema.sql)
- [data.sql](/E:/Year-4/Grad-Project/WayTrip/travel-server/src/main/resources/db/data.sql)

建议先创建数据库，再按顺序执行：

1. `schema.sql`
2. `data.sql`

### 4.3 Profile 说明

项目配置文件：

- [application.yml](/E:/Year-4/Grad-Project/WayTrip/travel-server/src/main/resources/application.yml)
- [application-dev.yml](/E:/Year-4/Grad-Project/WayTrip/travel-server/src/main/resources/application-dev.yml)
- [application-prod.yml](/E:/Year-4/Grad-Project/WayTrip/travel-server/src/main/resources/application-prod.yml)

默认激活的是：

```yaml
spring.profiles.active: prod
```

本地开发建议显式切到 `dev`：

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

或者：

```bash
mvn -Dspring.profiles.active=dev spring-boot:run
```

### 4.4 启动项目

编译：

```bash
mvn clean compile
```

启动：

```bash
mvn spring-boot:run
```

默认端口：

```text
http://localhost:8080
```

## 5. 常用开发命令

编译校验：

```bash
mvn -q -DskipTests test-compile
```

运行测试：

```bash
mvn test
```

打包：

```bash
mvn clean package
```

## 6. API 文档

项目启用了 SpringDoc / OpenAPI 3。
其中 `/v3/api-docs` 是 OpenAPI 3 的标准 JSON 描述接口，Swagger UI 会基于这份描述渲染文档页面。

启动后可访问：

- Swagger UI：`http://localhost:8080/swagger-ui/index.html`
- OpenAPI 文档：`http://localhost:8080/v3/api-docs`

接口分组：

- 用户端接口：`/api/v1/**`
- 管理端接口：`/api/admin/**`

相关配置见：

- [SwaggerConfig.java](/E:/Year-4/Grad-Project/WayTrip/travel-server/src/main/java/com/travel/config/web/SwaggerConfig.java)

## 7. 项目目录结构

当前后端主目录结构如下：

```text
travel-server
├─ .env                              # 本地或生产环境变量配置
├─ .env.example                      # 环境变量示例文件
├─ pom.xml                           # Maven 构建配置
├─ uploads/                          # 本地上传文件目录
├─ logs/                             # 应用运行日志目录
├─ src/main/java/com/travel/         # Java 主源码目录
│  ├─ TravelApplication.java         # Spring Boot 启动入口
│  ├─ common/                        # 通用能力
│  │  ├─ exception/                  # 全局异常与业务异常
│  │  └─ result/                     # 统一响应体、分页结果、错误码
│  ├─ config/                        # 配置层
│  │  ├─ aspect/                     # AOP 日志等切面配置
│  │  ├─ cache/                      # Redis 与缓存配置
│  │  ├─ integration/                # 外部集成相关配置
│  │  ├─ persistence/                # MyBatis-Plus 持久层配置
│  │  ├─ security/                   # 安全配置
│  │  └─ web/                        # WebMVC、Swagger 配置
│  ├─ constant/                      # 常量定义
│  ├─ controller/                    # 控制器层
│  │  ├─ admin/                      # 管理端接口
│  │  └─ app/                        # 用户端接口
│  ├─ dto/                           # 接口请求/响应对象
│  │  ├─ admin/                      # 管理员模块 DTO
│  │  ├─ auth/                       # 登录、注册、微信登录 DTO
│  │  ├─ banner/                     # 轮播图 DTO
│  │  ├─ category/                   # 景点分类 DTO
│  │  ├─ dashboard/                  # 后台看板 DTO
│  │  ├─ favorite/                   # 收藏 DTO
│  │  ├─ guide/                      # 攻略 DTO
│  │  │  └─ query/                   # 查询投影对象
│  │  ├─ home/                       # 首页 DTO
│  │  │  └─ item/                    # 响应项对象
│  │  ├─ order/                      # 订单 DTO
│  │  ├─ recommendation/             # 推荐模块 DTO
│  │  │  └─ config/                  # 推荐配置对象
│  │  ├─ region/                     # 地区 DTO
│  │  ├─ review/                     # 评价 DTO
│  │  │  └─ stats/                   # 评分统计对象
│  │  ├─ spot/                       # 景点 DTO
│  │  └─ user/                       # 用户 DTO
│  │     └─ item/                    # 后台用户洞察列表项
│  ├─ entity/                        # 数据库实体
│  ├─ enums/                         # 枚举定义
│  ├─ interceptor/                   # 拦截器
│  ├─ mapper/                        # MyBatis Mapper 接口
│  ├─ service/                       # 服务层
│  │  ├─ cache/                      # 缓存访问服务
│  │  ├─ impl/                       # 服务实现层
│  │  │  ├─ 门面服务实现              # 如 SpotServiceImpl、GuideServiceImpl
│  │  │  ├─ 业务子服务实现            # 如 Query/Admin/Behavior/Heat 拆分类
│  │  │  └─ 通用业务服务实现          # 如 Order/Review/Favorite 等
│  │  ├─ support/                    # 服务支撑类
│  │  │  ├─ recommendation/          # 推荐模块支撑类
│  │  │  │  ├─ 配置支撑               # 推荐配置读取与状态管理
│  │  │  │  ├─ 查询支撑               # 推荐结果组装与元信息补齐
│  │  │  │  ├─ 打分支撑               # 行为权重、候选重排、调试输出
│  │  │  │  ├─ 相似度支撑             # 相似邻居读取与离线矩阵更新
│  │  │  │  ├─ 冷启动支撑             # 冷启动兜底逻辑
│  │  │  │  └─ 浏览来源归类器         # 浏览来源归桶规则
│  │  │  ├─ spot/                    # 景点模块支撑类
│  │  │  │  ├─ 响应装配               # 景点响应对象组装
│  │  │  │  ├─ 树形处理               # 分类地区树与层级查询
│  │  │  │  └─ 写入支撑               # 后台写入字段映射与图片保存
│  │  │  └─ storage/                 # 上传模块支撑类
│  │  │     ├─ 上传校验               # 图片文件校验
│  │  │     ├─ 本地存储               # 文件落盘
│  │  │     ├─ 文件名生成             # 上传文件命名规则
│  │  │     └─ 落盘结果对象           # 已保存文件信息
│  │  └─ *.java                      # 各业务服务接口
│  ├─ task/                          # 定时任务
│  │  ├─ order/                      # 订单任务
│  │  ├─ recommendation/             # 推荐任务
│  │  └─ spot/                       # 景点任务
│  └─ util/                          # 工具类
│     ├─ mask/                       # 脱敏工具
│     ├─ security/                   # JWT 等安全工具
│     ├─ web/                        # 用户上下文工具
│     └─ wechat/                     # 微信接口调用工具
├─ src/main/resources/               # 资源文件目录
│  ├─ application.yml                # 通用配置
│  ├─ application-dev.yml            # 开发环境配置
│  ├─ application-prod.yml           # 生产环境配置
│  ├─ logback-spring.xml             # 日志配置
│  ├─ db/                            # 数据库初始化脚本
│  │  ├─ schema.sql                  # 建表脚本
│  │  └─ data.sql                    # 初始化数据脚本
│  └─ mapper/                        # MyBatis XML 映射文件
└─ target/                           # Maven 构建输出目录
```

## 8. 核心模块说明

### 8.1 用户与认证

主要职责：

- 用户注册、登录
- 微信小程序登录与手机号绑定
- 用户资料修改
- 用户密码修改与账户注销

关键类：

- [AuthController.java](/E:/Year-4/Grad-Project/WayTrip/travel-server/src/main/java/com/travel/controller/app/AuthController.java)
- [UserAccountController.java](/E:/Year-4/Grad-Project/WayTrip/travel-server/src/main/java/com/travel/controller/app/UserAccountController.java)
- [UserAuthServiceImpl.java](/E:/Year-4/Grad-Project/WayTrip/travel-server/src/main/java/com/travel/service/impl/UserAuthServiceImpl.java)
- [UserAccountServiceImpl.java](/E:/Year-4/Grad-Project/WayTrip/travel-server/src/main/java/com/travel/service/impl/UserAccountServiceImpl.java)
- [JwtUtils.java](/E:/Year-4/Grad-Project/WayTrip/travel-server/src/main/java/com/travel/util/security/JwtUtils.java)

### 8.2 景点模块

主要职责：

- 景点列表、搜索、详情、浏览历史
- 景点浏览行为上报
- 管理端景点增删改查
- 景点热度刷新

当前拆分方式：

- 门面类：
  - [SpotServiceImpl.java](/E:/Year-4/Grad-Project/WayTrip/travel-server/src/main/java/com/travel/service/impl/SpotServiceImpl.java)
- 子服务：
  - [SpotQueryServiceImpl.java](/E:/Year-4/Grad-Project/WayTrip/travel-server/src/main/java/com/travel/service/impl/SpotQueryServiceImpl.java)
  - [SpotAdminServiceImpl.java](/E:/Year-4/Grad-Project/WayTrip/travel-server/src/main/java/com/travel/service/impl/SpotAdminServiceImpl.java)
  - [SpotBehaviorServiceImpl.java](/E:/Year-4/Grad-Project/WayTrip/travel-server/src/main/java/com/travel/service/impl/SpotBehaviorServiceImpl.java)
  - [SpotHeatServiceImpl.java](/E:/Year-4/Grad-Project/WayTrip/travel-server/src/main/java/com/travel/service/impl/SpotHeatServiceImpl.java)
- 支撑类：
  - [SpotResponseAssembler.java](/E:/Year-4/Grad-Project/WayTrip/travel-server/src/main/java/com/travel/service/support/spot/SpotResponseAssembler.java)
  - [SpotTreeSupport.java](/E:/Year-4/Grad-Project/WayTrip/travel-server/src/main/java/com/travel/service/support/spot/SpotTreeSupport.java)
  - [SpotWriteSupport.java](/E:/Year-4/Grad-Project/WayTrip/travel-server/src/main/java/com/travel/service/support/spot/SpotWriteSupport.java)

### 8.3 攻略模块

主要职责：

- 攻略列表与详情
- 攻略预算分类查询
- 管理端攻略维护与发布控制

当前拆分方式：

- 门面类：
  - [GuideServiceImpl.java](/E:/Year-4/Grad-Project/WayTrip/travel-server/src/main/java/com/travel/service/impl/GuideServiceImpl.java)
- 子服务：
  - [GuideQueryServiceImpl.java](/E:/Year-4/Grad-Project/WayTrip/travel-server/src/main/java/com/travel/service/impl/GuideQueryServiceImpl.java)
  - [GuideAdminServiceImpl.java](/E:/Year-4/Grad-Project/WayTrip/travel-server/src/main/java/com/travel/service/impl/GuideAdminServiceImpl.java)

### 8.4 订单、评价、收藏、首页

相关能力：

- 订单创建、支付状态流转、后台订单查询
- 评价提交、删除、评分汇总
- 收藏新增与取消
- 首页热门景点、最近浏览、附近推荐

关键类：

- [OrderServiceImpl.java](/E:/Year-4/Grad-Project/WayTrip/travel-server/src/main/java/com/travel/service/impl/OrderServiceImpl.java)
- [ReviewServiceImpl.java](/E:/Year-4/Grad-Project/WayTrip/travel-server/src/main/java/com/travel/service/impl/ReviewServiceImpl.java)
- [FavoriteServiceImpl.java](/E:/Year-4/Grad-Project/WayTrip/travel-server/src/main/java/com/travel/service/impl/FavoriteServiceImpl.java)
- [DashboardServiceImpl.java](/E:/Year-4/Grad-Project/WayTrip/travel-server/src/main/java/com/travel/service/impl/DashboardServiceImpl.java)

### 8.5 推荐模块

推荐模块是当前后端里最复杂的一块，已经做过职责收口，现状是：

- 编排入口：
  - [RecommendationServiceImpl.java](/E:/Year-4/Grad-Project/WayTrip/travel-server/src/main/java/com/travel/service/impl/RecommendationServiceImpl.java)
- 缓存：
  - [RecommendationCacheService.java](/E:/Year-4/Grad-Project/WayTrip/travel-server/src/main/java/com/travel/service/cache/RecommendationCacheService.java)
- 支撑类：
  - [RecommendationConfigSupport.java](/E:/Year-4/Grad-Project/WayTrip/travel-server/src/main/java/com/travel/service/support/recommendation/RecommendationConfigSupport.java)
  - [RecommendationQuerySupport.java](/E:/Year-4/Grad-Project/WayTrip/travel-server/src/main/java/com/travel/service/support/recommendation/RecommendationQuerySupport.java)
  - [RecommendationScoreSupport.java](/E:/Year-4/Grad-Project/WayTrip/travel-server/src/main/java/com/travel/service/support/recommendation/RecommendationScoreSupport.java)
  - [RecommendationSimilaritySupport.java](/E:/Year-4/Grad-Project/WayTrip/travel-server/src/main/java/com/travel/service/support/recommendation/RecommendationSimilaritySupport.java)
  - [RecommendationColdStartSupport.java](/E:/Year-4/Grad-Project/WayTrip/travel-server/src/main/java/com/travel/service/support/recommendation/RecommendationColdStartSupport.java)
  - [RecommendationViewSourceClassifier.java](/E:/Year-4/Grad-Project/WayTrip/travel-server/src/main/java/com/travel/service/support/recommendation/RecommendationViewSourceClassifier.java)

推荐链路大致分为：

1. 读取推荐配置
2. 汇总用户浏览、收藏、评分、订单行为
3. 计算融合交互权重
4. 读取或构建相似景点邻居
5. 生成候选结果并过滤已交互内容
6. 做热度重排
7. 必要时降级到冷启动推荐
8. 将结果缓存到 Redis

### 8.6 上传模块

上传模块目前也做了职责分离：

- 对外服务：
  - [FileUploadService.java](/E:/Year-4/Grad-Project/WayTrip/travel-server/src/main/java/com/travel/service/FileUploadService.java)
- 支撑类：
  - [LocalFileStorageService.java](/E:/Year-4/Grad-Project/WayTrip/travel-server/src/main/java/com/travel/service/support/storage/LocalFileStorageService.java)
  - [ImageUploadValidator.java](/E:/Year-4/Grad-Project/WayTrip/travel-server/src/main/java/com/travel/service/support/storage/ImageUploadValidator.java)
  - [StorageFileNameGenerator.java](/E:/Year-4/Grad-Project/WayTrip/travel-server/src/main/java/com/travel/service/support/storage/StorageFileNameGenerator.java)
  - [StoredFileInfo.java](/E:/Year-4/Grad-Project/WayTrip/travel-server/src/main/java/com/travel/service/support/storage/StoredFileInfo.java)

## 9. 定时任务

当前定时任务包括：

- [OrderAutoCancelTask.java](/E:/Year-4/Grad-Project/WayTrip/travel-server/src/main/java/com/travel/task/order/OrderAutoCancelTask.java)
  - 自动取消超时未支付订单
- [SpotHeatSyncTask.java](/E:/Year-4/Grad-Project/WayTrip/travel-server/src/main/java/com/travel/task/spot/SpotHeatSyncTask.java)
  - 定时同步景点热度
- [RecommendationMatrixRefreshTask.java](/E:/Year-4/Grad-Project/WayTrip/travel-server/src/main/java/com/travel/task/recommendation/RecommendationMatrixRefreshTask.java)
  - 定时重建推荐相似度矩阵

## 10. 配置说明

### 10.1 数据库

开发环境默认指向本地：

```text
jdbc:mysql://localhost:3306/waytrip_db
```

生产环境通过 `.env` 注入：

- `DB_HOST`
- `DB_PORT`
- `DB_NAME`
- `DB_USERNAME`
- `DB_PASSWORD`

### 10.2 Redis

主要用于：

- 用户推荐结果缓存
- 景点相似度缓存
- 推荐配置缓存
- 推荐状态摘要缓存

生产环境通过 `.env` 注入：

- `REDIS_HOST`
- `REDIS_PORT`
- `REDIS_PASSWORD`

### 10.3 JWT

配置项：

- `jwt.secret`
- `jwt.expiration`
- `jwt.admin-expiration`

生产环境至少要保证：

- `JWT_SECRET` 非空
- 长度不少于 32 个字符

### 10.4 微信小程序

配置项：

- `WECHAT_APPID`
- `WECHAT_SECRET`

相关调用类：

- [WxApiClient.java](/E:/Year-4/Grad-Project/WayTrip/travel-server/src/main/java/com/travel/util/wechat/WxApiClient.java)

### 10.5 上传目录

上传目录配置：

- `UPLOAD_PATH`

默认值：

```text
./uploads
```

## 11. 推荐浏览来源归类

景点详情浏览上报接口：

- `POST /api/v1/spots/{spotId}/view`

前端会按页面入口传递 `source`，后端在推荐计算时再归类到有限的来源桶。

归类逻辑当前实现在：

- [RecommendationViewSourceClassifier.java](/E:/Year-4/Grad-Project/WayTrip/travel-server/src/main/java/com/travel/service/support/recommendation/RecommendationViewSourceClassifier.java)

### 11.1 前端可能上传的来源值

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

### 11.2 后端归类结果

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

### 11.3 维护约定

1. 新增景点详情入口时，先确认前端 `source` 是否有独立统计价值。
2. 如果只需要保留页面语义，可以新增来源值，再由后端统一归桶。
3. 如果新来源会影响推荐权重，必须同步更新来源归类规则。
4. 不要把这类归类规则做成后台运营配置，避免开发约定和业务配置混在一起。

## 12. 开发约定

### 12.1 目录约定

- `controller` 只承接接口，不下沉业务细节
- `service/impl` 放主业务实现或门面实现
- `service/support` 放被多个服务复用的支撑逻辑
- `dto` 按模块拆分，优先使用 `client / response / query / item / stats / config`
- `util` 只放真正通用、无业务状态的工具类

### 12.2 推荐的开发方式

- 新增接口时，优先补 DTO，不要直接在 controller 上写散乱参数
- 新增复杂业务时，优先拆到具体 `ServiceImpl` 或 `support`，不要继续把逻辑堆进门面类
- 新增注释只写关键原因，不写翻译代码式注释
- 涉及 Redis 缓存结构变更时，记得同步考虑旧缓存清理

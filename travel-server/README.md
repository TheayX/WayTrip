# travel-server

WayTrip 后端服务，基于 Spring Boot 3 + MyBatis-Plus + Redis，统一承接用户端与管理端接口、推荐计算、文件上传和后台管理能力。

## 技术栈

- Java 17
- Spring Boot 3.5.11
- MyBatis-Plus 3.5.5
- MySQL
- Redis
- JWT
- SpringDoc / OpenAPI 3

## 快速开始

### 环境要求

- JDK 17+
- Maven 3.9+
- MySQL 8.x
- Redis 6.x 或 7.x

### 环境变量

后端采用 `.env.example -> .env` 约定。

```bash
cp .env.example .env
```

Windows PowerShell 下可手动创建 `.env`。当前模板覆盖：

- 数据库连接
- Redis 连接与连接池
- JWT 密钥
- 微信小程序配置
- 推荐缓存与任务调度参数
- 上传目录

补充说明：

- 默认 profile 为 `prod`
- 如果继续使用 `prod`，则在 `travel-server` 下创建 `.env` 并按模板填写
- 如果切到 `dev`，则直接在 `src/main/resources/application-dev.yml` 中填写本地数据库、Redis、JWT、微信与上传目录配置
- `UPLOAD_PATH` 建议优先使用绝对路径；如果继续使用相对路径，请固定启动方式

### 数据库初始化

执行以下脚本：

1. [schema.sql](./src/main/resources/db/schema.sql)
2. [data.sql](./src/main/resources/db/data.sql)

### 启动项目

```bash
mvn spring-boot:run
```

默认地址：

- API：`http://localhost:8080`
- Swagger UI：`http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON：`http://localhost:8080/v3/api-docs`

### Profile 说明

- 默认 `spring.profiles.active=prod`
- 本地开发可显式切到 `dev`

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

## 目录结构

```text
src/main/java/com/travel/
├─ common/                          通用结果、异常等
├─ config/                          配置层
├─ constant/                        常量
├─ controller/                      接口层
├─ dto/                             请求与响应对象
├─ entity/                          实体
├─ enums/                           枚举
├─ interceptor/                     拦截器
├─ mapper/                          MyBatis Mapper
├─ service/                         服务与支撑逻辑
├─ task/                            定时任务
└─ util/                            工具类
```

## 主要能力

- 用户端与管理端 JWT 鉴权
- 用户认证、资料、偏好、密码、注销
- 景点、攻略、评论、收藏、订单、轮播图、地区、分类
- 文件上传与资源访问
- 用户行为记录、热度同步、推荐计算与缓存
- 仪表板、用户洞察、管理员管理、推荐调试

## 配置要点

- 用户端接口前缀：`/api/v1/*`
- 管理端接口前缀：`/api/admin/v1/*`
- 推荐相关缓存、状态和相似度矩阵统一写入 Redis
- 上传路径由 `UPLOAD_PATH` 控制，建议优先使用绝对路径

## 常用命令

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

## 相关文档

- [根说明](../README.md)
- [设计文档](../docs/specs/travel-recommendation-system/design.md)
- [API 文档](../docs/specs/travel-recommendation-system/api.md)
- [数据库文档](../docs/specs/travel-recommendation-system/database.md)

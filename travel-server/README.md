# travel-server

WayTrip 后端服务，统一承接用户端与管理端接口、推荐计算、文件上传和后台管理能力。

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

### 环境配置

后端采用 `.env.example -> .env` 约定。

```bash
cp .env.example .env
```

模板覆盖以下配置：

- 数据库连接
- Redis 连接与连接池
- JWT 密钥
- 微信小程序配置
- 推荐缓存与任务调度参数
- 上传目录

补充说明：

- 默认 profile 为 `prod`
- 若继续使用 `prod`，在当前目录创建 `.env`
- 若切到 `dev`，直接修改 `src/main/resources/application-dev.yml`
- `UPLOAD_PATH` 建议优先使用绝对路径

### 数据库初始化

按顺序执行以下脚本：

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
├─ common/                          通用结果与异常
├─ config/                          配置层
│  ├─ cache/                        缓存配置
│  ├─ persistence/                  持久层配置
│  ├─ security/                     安全配置
│  └─ web/                          Web 配置
├─ controller/                      接口层
│  ├─ admin/                        管理端接口
│  └─ app/                          用户端接口
├─ dto/                             请求与响应对象
├─ entity/                          实体
├─ mapper/                          MyBatis Mapper
├─ service/                         业务服务
│  ├─ impl/                         服务实现
│  └─ support/                      业务支撑逻辑
├─ task/                            定时任务
│  ├─ order/                        订单任务
│  ├─ recommendation/               推荐任务
│  └─ spot/                         景点任务
└─ util/                            工具类
```

服务模块内部按职责拆分：

```text
service/
├─ XxxService.java
├─ impl/
│  └─ XxxServiceImpl.java
└─ support/                         按领域沉淀复用支撑逻辑
```

## 主要能力

- 用户端与管理端 JWT 鉴权
- 用户认证、资料、偏好、密码、注销
- 景点、攻略、评论、收藏、订单、轮播图、地区、分类
- 用户浏览行为记录、热度同步、推荐计算与缓存
- 仪表板、用户洞察、管理员管理、推荐调试
- 文件上传与静态资源访问

## 接口约定

- 用户端接口前缀：`/api/v1/*`
- 管理端接口前缀：`/api/admin/v1/*`
- 用户资料主入口：`/api/v1/user/*`
- 推荐相关缓存、状态和相似度矩阵统一写入 Redis

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

- [仓库总览](../README.md)
- [设计文档](../docs/specs/travel-recommendation-system/design.md)
- [API 文档](../docs/specs/travel-recommendation-system/api.md)
- [数据库文档](../docs/specs/travel-recommendation-system/database.md)

# travel-server

WayTrip 后端服务，统一承接用户端与管理端接口、推荐计算、文件上传、后台管理与 AI 聊天能力。

## 技术栈

- Java 17
- Spring Boot 3.5.11
- Spring MVC
- MyBatis-Plus 3.5.5
- MySQL
- Redis
- JWT
- SpringDoc / OpenAPI 3
- Maven
- Spring AI 1.0.x

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
- AI 三段模型配置（生成 / 意图 / embedding）
- AI 对话、RAG、记忆与限流参数
- AI 向量库 Redis 连接参数

补充说明：

- 默认 profile 为 `prod`
- 若继续使用 `prod`，在当前目录创建 `.env`
- 若切到 `dev`，直接修改 `src/main/resources/application-dev.yml`
- `UPLOAD_PATH` 建议优先使用绝对路径
- 模板默认开启 `APP_AI_RAG_ENABLED=true`，方便直接联调 AI 客服
- 向量库 Redis 默认使用 `6380` 端口，可与业务 Redis 分离部署

### 数据库初始化

按顺序执行以下脚本：

1. [schema.sql](./src/main/resources/db/schema.sql)
2. [data.sql](./src/main/resources/db/data.sql)
3. [seed/10_region.sql](./src/main/resources/db/seed/10_region.sql)（补充主要省份与核心城市）

如果需要追加扩容数据，再按需执行：

- [db/seed/bulk](./src/main/resources/db/seed/bulk)

当前手工扩容顺序建议：

1. [10_spot.sql](./src/main/resources/db/seed/bulk/10_spot.sql)
2. [20_user.sql](./src/main/resources/db/seed/bulk/20_user.sql)
3. [30_user_preference.sql](./src/main/resources/db/seed/bulk/30_user_preference.sql)
4. [40_user_spot_favorite.sql](./src/main/resources/db/seed/bulk/40_user_spot_favorite.sql)
5. [50_user_spot_view.sql](./src/main/resources/db/seed/bulk/50_user_spot_view.sql)
6. [60_user_spot_review.sql](./src/main/resources/db/seed/bulk/60_user_spot_review.sql)
7. [70_order.sql](./src/main/resources/db/seed/bulk/70_order.sql)
8. [80_guide_enhance.sql](./src/main/resources/db/seed/bulk/80_guide_enhance.sql)

补充说明：

- `data.sql` 用于基础演示数据初始化，会清空并重建基础数据。
- 如果数据库里已经有你在后台手工改过的景点图片或其他基础内容，不要重复执行 `data.sql`。
- `seed/10_region.sql` 与 `db/seed/bulk/*.sql` 约定为追加型扩容数据，只插入新 ID，不覆盖、不清空、不修改已有基础数据。
- 当前扩容 SQL 默认依赖 MySQL 8，部分文件使用递归 CTE 生成批量数据。

### 启动项目

```bash
mvn spring-boot:run
```

日志路径补充（避免出现 `travel-server/travel-server/logs`）：

- 日志根目录由 `LOG_HOME` 控制，未配置时使用 `logback-spring.xml` 的默认值。
- 相对路径会基于启动工作目录（`cwd`）解析，因此不同启动方式可能落到不同位置。
- 当前默认逻辑已兼容 VS Code/IDEA 常见启动目录，未显式设置时也会统一落到 `travel-server/logs`；如需自定义，再在运行配置里设置 `LOG_HOME`。

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
travel-server/
├─ .env.example                     环境变量模板，定义数据库、Redis、JWT、微信和上传配置
├─ .env                             当前环境实际配置文件，由开发者或部署环境自行创建
├─ .gitignore                       Git 忽略规则
├─ pom.xml                          Maven 构建入口，管理依赖、插件和打包行为
├─ src/
│  ├─ main/
│  │  ├─ java/com/travel/
│  │  │  ├─ TravelApplication.java  Spring Boot 启动入口
│  │  │  ├─ common/                 通用响应、常量文本与异常处理
│  │  │  ├─ config/                 应用配置层
│  │  │  │  ├─ ai/                  AI 模型、向量库与运行时配置
│  │  │  │  ├─ aspect/              AOP 日志等横切配置
│  │  │  │  ├─ cache/               Redis、缓存键与缓存参数配置
│  │  │  │  ├─ integration/         外部 HTTP 客户端等集成配置
│  │  │  │  ├─ persistence/         MyBatis-Plus 与字段填充配置
│  │  │  │  ├─ security/            安全相关配置
│  │  │  │  ├─ task/                定时任务调度配置
│  │  │  │  └─ web/                 MVC、Swagger 等 Web 层配置
│  │  │  ├─ constant/               业务常量
│  │  │  ├─ controller/             接口层
│  │  │  │  ├─ admin/               管理端接口，如后台登录、景点、订单、推荐、上传、AI 知识维护
│  │  │  │  └─ app/                 用户端接口，如首页、景点、攻略、订单、账户与 AI 聊天
│  │  │  ├─ dto/                    请求参数、响应对象与缓存/配置传输对象，含 AI 请求/响应 DTO
│  │  │  ├─ entity/                 数据库实体
│  │  │  ├─ enums/                  枚举定义，如订单状态、AI 场景与知识状态
│  │  │  ├─ interceptor/            登录鉴权拦截器
│  │  │  ├─ mapper/                 MyBatis Mapper 接口
│  │  │  ├─ service/                业务服务层
│  │  │  │  ├─ ai/                  AI 聊天、知识检索、工具、规则提供器
│  │  │  │  ├─ cache/               推荐缓存等缓存服务
│  │  │  │  ├─ impl/                服务实现
│  │  │  │  └─ support/             领域支撑逻辑，如 admin、spot、recommendation、storage
│  │  │  ├─ task/                   定时任务实现，如订单自动取消、热度同步、推荐矩阵刷新
│  │  │  └─ util/                   通用工具，如 JWT、脱敏、微信调用、上下文处理
│  │  └─ resources/
│  │     ├─ application.yml         基础配置入口，负责导入 .env 并指定默认 profile
│  │     ├─ application-dev.yml     本地开发环境配置
│  │     ├─ application-prod.yml    生产环境配置
│  │     ├─ logback-spring.xml      日志输出配置
│  │     ├─ db/                     建表、基础数据与扩容 SQL
│  │     └─ mapper/                 MyBatis XML 映射文件
│  └─ test/
│     └─ java/com/travel/
│        ├─ controller/             控制器测试，含管理端 AI 知识接口测试
│        ├─ service/ai/             AI 聊天、意图识别、RAG、知识导入相关测试
│        ├─ service/impl/           服务层单元测试
│        └─ web/                    Web 层与拦截器相关测试
├─ scripts/                         辅助脚本，当前包含历史批量数据生成脚本
├─ uploads/                         本地上传资源目录，包含默认图片与图标
├─ logs/                            运行日志目录
├─ target/                          Maven 构建产物目录
└─ travel-server.iml                IDE 工程文件
```

服务模块内部按职责拆分：

```text
service/
├─ XxxService.java
├─ cache/                           可选，仅在存在独立缓存职责时创建
├─ ai/
│  ├─ chat/                         AI 对话编排与提示词
│  ├─ memory/                       对话记忆
│  ├─ rag/                          知识检索与上下文增强
│  ├─ rule/                         AI 可消费的业务规则真相源摘要
│  └─ tool/                         AI 工具与工具注册
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
- Spring AI 驱动的聊天、RAG 检索、工具调用与规则摘要

## AI 模块说明

当前 AI 模块已经按“主聊天链路 + 三段模型 + 双轨 RAG”结构收口：

- 核心技术：`Spring AI`、`RAG`、`SSE`、`Function Calling`、`Redis Vector Store`、`Redis Stream`
- 模型接入：最终生成默认走 OpenAI 兼容接口，意图识别和 Embedding 默认走本地 Ollama
- 文档分层：本 README 只说明后端实现概览，完整说明见 [docs/ai-chat-service.md](../docs/ai-chat-service.md)

- `config/ai/`
  - `AiModelConfig`：显式拆分生成、意图、嵌入三段模型，并注册流式线程池、双轨执行器和向量库
  - `AiProperties`：统一收口 AI 环境变量
- `service/ai/chat/`
  - `AiConversationService`：主聊天编排入口，负责风控、场景路由、双轨并行、流式输出和指标记录
  - `AiPromptManager`：系统提示词与场景提示组装
  - `AiScenarioRouter`：显式场景提示与兜底路由
  - `AiContextFusionService`：融合 RAG、工具和业务上下文
  - `AiConversationContextService`：组装对话期业务上下文
  - `AiResponseAssembler`：统一响应事件封装
- `service/ai/intent/`
  - `AiIntentService`：运行时意图识别与槽位提取
- `service/ai/rag/`
  - `AiKnowledgeRetrievalService`：知识检索接口
  - `RedisVectorAiKnowledgeRetrievalService`：基于 Redis 向量库的检索实现
  - `AiKnowledgeContextAdvisor`：知识上下文增强
  - `AiKnowledgeAdminServiceImpl`：管理端知识预览与检索联调入口
- `controller/app/ai/`
  - `AiChatController`：对外聊天入口，负责 SSE 流式输出
  - `AiSessionController`：会话创建与会话列表能力
  - `AiFeedbackController`：用户反馈提交入口
- `controller/admin/`
  - `AdminAiKnowledgeController`：知识文档维护、preview、向量索引状态与导入任务入口
- `service/ai/memory/`
  - `RedisChatMemory`：基于 Redis 的对话记忆
  - `AiSessionIdService`：统一会话标识管理
- `service/ai/guardrail/`
  - `AiGuardrailService`：限流、登录边界和前置规则拦截
- `service/ai/tool/`
  - `AiToolExecutionService`：统一工具执行入口
  - `AiToolRegistry`：工具暴露注册中心
  - `*AiTools`：订单、推荐、景点等业务工具集合
- `service/ai/rule/`
  - `*RuleProvider`：AI 可消费的业务规则真相源摘要

当前主链路约定：

- 左轨负责 RAG 检索
- 右轨负责业务上下文与工具预处理
- 两轨并行后再进入最终生成模型
- 管理端 preview 支持查看多知识域命中结果，而不是只看单一默认知识域
- 对外回复通过 `SSE` 流式输出，工具事实通过受控调用链补充，避免模型直接编造业务数据

规则边界约定：
- 订单状态、退款能力、超时阈值等真实业务规则来自 Java 真相源；
- `data.sql`、知识库文案与攻略数据主要用于内容知识、样本数据与 RAG；
- AI 不能从样本数据“猜测”真实规则。

## 接口约定

- 用户端接口前缀：`/api/v1/*`
- 管理端接口前缀：`/api/admin/v1/*`
- 用户资料主入口：`/api/v1/user/*`
- 推荐相关缓存、状态和相似度矩阵统一写入 Redis

## 常用命令

编译校验：

```bash
mvn -q -DskipTests compile
```

运行测试：

```bash
mvn test
```

打包：

```bash
mvn clean package
```

扩容数据维护：

- 当前主流程以 `src/main/resources/db/seed/` 下的手工 SQL 为准。
- `scripts/` 下的历史生成脚本不属于当前推荐导库流程。

## 文档后续可补充

- 包结构说明：补充 `controller / service / support / task` 之间的调用边界
- Redis 键说明：补充推荐缓存、状态缓存、矩阵缓存的键命名与用途
- 上传说明：补充 `uploads/` 目录约定、默认资源与访问路径
- 测试说明：补充当前测试覆盖范围和执行前置条件

## 相关文档

- [仓库总览](../README.md)
- [设计文档](../docs/specs/travel-recommendation-system/design.md)
- [API 文档](../docs/specs/travel-recommendation-system/api.md)
- [数据库文档](../docs/specs/travel-recommendation-system/database.md)
- [AI 聊天服务说明](../docs/ai-chat-service.md)

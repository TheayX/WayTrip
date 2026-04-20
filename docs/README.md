# docs

`docs/` 用于收口 WayTrip 的正式项目文档。根 README 只保留总览与启动入口，细节说明统一以下沉文档为准。

## 文档索引

### 开发与联调

- [开发期 HTTPS 与反代说明](./dev-https-proxy.md)
- [资源可见性与展示语义约定](./resource-visibility-display.md)
- [AI 客服架构与实现说明](./ai-chat-service.md)

### 规格说明

- [需求文档](./specs/travel-recommendation-system/requirements.md)
- [设计文档](./specs/travel-recommendation-system/design.md)
- [API 文档](./specs/travel-recommendation-system/api.md)
- [数据库文档](./specs/travel-recommendation-system/database.md)
- [任务文档](./specs/travel-recommendation-system/tasks.md)

## 维护约定

- 正式、长期保留的项目文档统一放在 `docs/`
- 根 README 只保留仓库总览、启动入口与文档链接
- 子项目 README 只描述各自运行方式、目录结构和开发约定
- 规格文档以 `docs/specs/travel-recommendation-system/` 为唯一正式目录
- 更新功能、接口、数据结构或项目状态时，同步修改对应规格文档，不在多处保留相互冲突的副本

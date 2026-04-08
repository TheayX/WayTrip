# WayTrip 开发期 HTTPS 与反代说明

本文档只说明开发联调阶段的推荐做法，目标是两点：

1. 默认保持“拉下来即可运行”，不要求每个人先配 Nginx。
2. 需要模拟 HTTPS、微信小程序去除 HTTP 警告、或使用 ngrok 时，只需追加少量配置。

## 1. 默认开发模式

这是项目默认推荐模式，不做额外反代。

### 后端

- 直接本地启动 `http://localhost:8080`

### Web 用户端

- 启动 `travel-web`
- Vite 开发服务器默认端口 `3001`
- `/api`、`/uploads` 通过 Vite proxy 转发到 `http://localhost:8080`

### 管理端

- 启动 `travel-admin`
- Vite 开发服务器默认端口 `3000`
- `/api`、`/uploads` 通过 Vite proxy 转发到 `http://localhost:8080`

### 小程序端

- 启动 `travel-miniapp`
- 默认接口地址为 `http://localhost:8080`
- 微信开发者工具下如果图片或资源来自 HTTP，可能出现“请升级到 HTTPS”的警告

说明：

- 当前项目默认允许该警告存在，因为本地开发阶段仍可继续联调。
- 这样做的好处是新同学拉取仓库后不需要先配置 Nginx。

## 2. 可选 HTTPS 反代模式

如果需要尽量贴近正式环境，或者希望消除微信开发者工具中的 HTTP 警告，可以启用本机 Nginx 反代。

推荐只给后端接口统一补一层 HTTPS：

```text
https://localhost:8443  ->  http://localhost:8080
```

这样已经足够满足小程序端常见联调需求，Web 和管理端不必默认一起反代。

### 小程序端配置

先复制 `travel-miniapp/.env.example` 为 `travel-miniapp/.env.local`，再写入：

```env
VITE_API_ORIGIN=https://localhost:8443
```

切回默认本地开发时，改回：

```env
VITE_API_ORIGIN=http://localhost:8080
```

## 3. Web 与管理端可选反代

只有在你确实需要以下场景时，才建议给 Web 或管理端补反代：

- 想用 Nginx 统一模拟正式站点入口
- 想把本地前端服务继续暴露给 ngrok
- 需要验证浏览器侧 HTTPS、Cookie、跨域或反向代理链路

此时可以通过环境变量临时启用，不需要改代码。

### Web 端可选环境变量

先复制 `travel-web/.env.example` 为 `travel-web/.env.local`，再按需修改：

```env
VITE_DEV_PROXY_TARGET=https://localhost:8443
VITE_DEV_HOST=127.0.0.1
VITE_DEV_ALLOWED_HOSTS=你的-ngrok-域名
```

### 管理端可选环境变量

先复制 `travel-admin/.env.example` 为 `travel-admin/.env.local`，再按需修改：

```env
VITE_DEV_PROXY_TARGET=https://localhost:8443
VITE_DEV_HOST=127.0.0.1
VITE_DEV_ALLOWED_HOSTS=你的-ngrok-域名
```

说明：

- `VITE_DEV_PROXY_TARGET` 用于把 `/api` 和 `/uploads` 改为走 HTTPS 反代入口。
- `VITE_DEV_HOST=127.0.0.1` 用于解决部分场景下 Vite 监听 `::1`，而 Nginx 反代 `127.0.0.1` 时不一致的问题。
- `VITE_DEV_ALLOWED_HOSTS` 只在需要外部域名访问 Vite 开发服务时配置，例如 ngrok。

## 4. 更接近正式环境的模拟方式

如果是为了毕业设计答辩前做一次更规范的“模拟部署”，更推荐下面这条链路：

```text
Nginx(HTTPS)
  ├─ /           -> Web 或管理端构建后的 dist
  ├─ /api        -> Spring Boot 8080
  └─ /uploads    -> Spring Boot 8080
```

原因：

- 这比 `Nginx -> Vite dev server` 更接近真实生产结构。
- 可以验证静态资源托管、接口反代和 HTTPS 入口是否正常。
- 也避免把“开发服务器特性”误当成“发布形态”。

## 5. 当前项目建议

当前项目建议采用下面的日常策略：

- 平时开发：直接本地启动后端、Web、管理端，不强依赖 Nginx。
- 小程序联调：默认允许 HTTP 警告；需要更干净时再把 `VITE_API_ORIGIN` 切到 `https://localhost:8443`。
- 演示或模拟部署：优先用 Nginx 托管构建产物，而不是长期维护 `Nginx -> Vite`。
